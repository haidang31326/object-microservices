const API_BASE_URL = 'http://localhost:8090/api/v1';
const ORDERS_API_URL = 'http://localhost:8090/orders';

let keycloak = null;

document.addEventListener('DOMContentLoaded', async () => {
    // 1. Initialize Keycloak
    try {
        keycloak = new Keycloak({
            url: 'http://localhost:8091',
            realm: 'ticketing',
            clientId: 'ticketing-client' // Assuming this is the client ID configured in Keycloak
        });

        const authenticated = await keycloak.init({ onLoad: 'check-sso' });
        
        if (authenticated) {
            document.getElementById('btn-login').style.display = 'none';
            document.getElementById('user-info').classList.remove('hidden');
            document.getElementById('user-name').textContent = keycloak.tokenParsed.preferred_username || keycloak.tokenParsed.email || 'User';
            
            // Check if user has admin role (optional, depending on keycloak setup)
            const roles = keycloak.tokenParsed.realm_access?.roles || [];
            if (roles.includes('admin') || roles.includes('ADMIN')) {
                document.getElementById('admin-tab-btn').style.display = 'inline-block';
            } else {
                // Show it anyway for demo purposes
                document.getElementById('admin-tab-btn').style.display = 'inline-block';
            }
        }
    } catch (e) {
        console.error('Failed to initialize Keycloak', e);
    }

    // Auth Buttons
    document.getElementById('btn-login').addEventListener('click', () => {
        if(keycloak) keycloak.login();
    });
    document.getElementById('btn-logout').addEventListener('click', () => {
        if(keycloak) keycloak.logout();
    });

    // Helper for fetch with Auth Token
    const fetchWithAuth = async (url, options = {}) => {
        if (keycloak && keycloak.authenticated) {
            await keycloak.updateToken(30);
            options.headers = {
                ...options.headers,
                'Authorization': `Bearer ${keycloak.token}`
            };
        }
        return fetch(url, options);
    };

    // Tabs Logic
    const tabBtns = document.querySelectorAll('.tab-btn');
    const tabContents = document.querySelectorAll('.tab-content');

    tabBtns.forEach(btn => {
        btn.addEventListener('click', () => {
            tabBtns.forEach(b => b.classList.remove('active'));
            tabContents.forEach(c => c.classList.remove('active', 'hidden'));
            tabContents.forEach(c => c.classList.add('hidden'));

            btn.classList.add('active');
            document.getElementById(btn.dataset.target).classList.remove('hidden');
            document.getElementById(btn.dataset.target).classList.add('active');
        });
    });

    // --- Book Ticket Tab ---
    const searchBtn = document.getElementById('btn-search-event');
    const eventInput = document.getElementById('event-id-input');
    const eventDetails = document.getElementById('event-details');
    
    searchBtn.addEventListener('click', async () => {
        const eventId = eventInput.value;
        if (!eventId) return alert('Please enter an Event ID');

        searchBtn.disabled = true;
        searchBtn.textContent = 'Searching...';

        try {
            const response = await fetchWithAuth(`${API_BASE_URL}/inventory/event/${eventId}`);
            if (!response.ok) throw new Error('Event not found or server error');

            const data = await response.json();
            document.getElementById('res-event-name').textContent = data.event || `Event #${data.eventId}`;
            document.getElementById('res-event-capacity').textContent = data.capacity || '-';
            document.getElementById('res-event-left').textContent = data.capacity !== undefined ? data.capacity : '-'; // inventoryService returns capacity mapping to leftCapacity
            document.getElementById('res-event-price').textContent = data.ticketPrice ? `$${data.ticketPrice}` : '-';
            
            eventDetails.classList.remove('hidden');
            document.getElementById('book-event-id').value = eventId; // Use entered ID since response might not have it if mapped incorrectly
        } catch (error) {
            alert(error.message);
            eventDetails.classList.add('hidden');
        } finally {
            searchBtn.disabled = false;
            searchBtn.textContent = 'Search';
        }
    });

    const bookingForm = document.getElementById('booking-form');
    const bookingMessage = document.getElementById('booking-message');

    bookingForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        const userId = document.getElementById('user-id').value;
        const eventId = document.getElementById('book-event-id').value;
        const ticketCount = document.getElementById('ticket-count').value;

        const submitBtn = document.getElementById('btn-book');
        submitBtn.disabled = true;
        bookingMessage.className = 'message hidden';

        try {
            const response = await fetchWithAuth(`${API_BASE_URL}/booking`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    userId: parseInt(userId),
                    eventId: parseInt(eventId),
                    ticketCount: parseInt(ticketCount)
                })
            });

            if (!response.ok) throw new Error(await response.text() || 'Booking failed');

            const data = await response.json();
            bookingMessage.textContent = `Success! Booked ${data.ticketCount} ticket(s). Total: $${data.totalPrice}`;
            bookingMessage.classList.remove('hidden');
            bookingMessage.classList.add('success');
            
            bookingForm.reset();
            eventDetails.classList.add('hidden');
            eventInput.value = '';
        } catch (error) {
            bookingMessage.textContent = error.message;
            bookingMessage.classList.remove('hidden');
            bookingMessage.classList.add('error');
        } finally {
            submitBtn.disabled = false;
            submitBtn.textContent = 'Confirm Booking';
        }
    });

    // --- Order History Tab ---
    const fetchOrdersBtn = document.getElementById('btn-fetch-orders');
    const ordersList = document.getElementById('orders-list');

    fetchOrdersBtn.addEventListener('click', async () => {
        const userId = document.getElementById('history-user-id').value;
        if(!userId) return alert("Enter User ID");

        ordersList.innerHTML = '<p>Loading...</p>';
        try {
            const response = await fetchWithAuth(`${ORDERS_API_URL}/history?CustomerID=${userId}`);
            if(!response.ok) throw new Error("Failed to fetch orders");
            
            const orders = await response.json();
            ordersList.innerHTML = '';
            
            if(orders.length === 0) {
                ordersList.innerHTML = '<p>No orders found.</p>';
                return;
            }

            orders.forEach(order => {
                const item = document.createElement('div');
                item.className = 'order-item';
                item.innerHTML = `
                    <div class="order-info">
                        <strong>Order #${order.id || order.id}</strong>
                        <span>Event ID: ${order.eventId}</span>
                        <span>Tickets: ${order.ticketCount}</span>
                        <span>Total: $${order.totalPrice}</span>
                    </div>
                    <button class="btn-danger" onclick="cancelOrder(${order.id || order.id})">Cancel</button>
                `;
                ordersList.appendChild(item);
            });
        } catch(e) {
            ordersList.innerHTML = `<p style="color:var(--error)">${e.message}</p>`;
        }
    });

    window.cancelOrder = async (orderId) => {
        if(!confirm(`Are you sure you want to cancel order #${orderId}?`)) return;
        
        try {
            const res = await fetchWithAuth(`${ORDERS_API_URL}/${orderId}/cancel`, { method: 'DELETE' });
            if(!res.ok) throw new Error("Failed to cancel order");
            alert("Order canceled successfully");
            fetchOrdersBtn.click(); // reload
        } catch(e) {
            alert(e.message);
        }
    };

    // --- Admin Tab ---
    const adminActionSelect = document.getElementById('admin-action-select');
    const adminForm = document.getElementById('admin-form');
    const adminMsg = document.getElementById('admin-message');

    adminActionSelect.addEventListener('change', (e) => {
        const action = e.target.value;
        document.getElementById('admin-event-id-group').style.display = action === 'create' ? 'none' : 'block';
        
        const isDelete = action === 'delete';
        document.querySelector('.name-group').style.display = isDelete ? 'none' : 'block';
        document.querySelector('.capacity-group').style.display = isDelete ? 'none' : 'block';
        document.querySelector('.price-group').style.display = isDelete ? 'none' : 'block';
        document.getElementById('admin-venue-id-group').style.display = action === 'create' ? 'block' : 'none';
        
        // Remove required attributes for hidden fields to allow form submission
        const requiredFields = ['admin-venue-id', 'admin-event-name', 'admin-event-total-cap', 'admin-event-left-cap', 'admin-event-price'];
        requiredFields.forEach(id => document.getElementById(id).required = !isDelete);
        if(action !== 'create') document.getElementById('admin-venue-id').required = false;
    });

    adminForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        const action = adminActionSelect.value;
        const eventId = document.getElementById('admin-event-id').value;
        const venueId = document.getElementById('admin-venue-id').value;
        
        const payload = {
            id: eventId ? parseInt(eventId) : null,
            name: document.getElementById('admin-event-name').value,
            totalCapacity: parseInt(document.getElementById('admin-event-total-cap').value),
            leftCapacity: parseInt(document.getElementById('admin-event-left-cap').value),
            price: parseFloat(document.getElementById('admin-event-price').value)
        };

        let url = `${API_BASE_URL}/inventory/event`;
        let method = 'POST';

        if(action === 'create') {
            url += `/create/venue/${venueId}`;
        } else if (action === 'update') {
            url += `/${eventId}`;
            method = 'PUT';
        } else if (action === 'delete') {
            url += `/${eventId}`;
            method = 'DELETE';
        }

        adminMsg.className = 'message hidden';
        try {
            const res = await fetchWithAuth(url, {
                method,
                headers: { 'Content-Type': 'application/json' },
                body: method !== 'DELETE' ? JSON.stringify(payload) : null
            });

            if(!res.ok) throw new Error(`Failed to ${action} event`);
            
            adminMsg.textContent = `Successfully ${action}d event`;
            adminMsg.classList.remove('hidden');
            adminMsg.classList.add('success');
            adminForm.reset();
        } catch(err) {
            adminMsg.textContent = err.message;
            adminMsg.classList.remove('hidden');
            adminMsg.classList.add('error');
        }
    });
});
