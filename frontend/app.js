// =============================================================================
// EventTick - Frontend Redesigned Application
// API Gateway: http://localhost:8090
// Keycloak:    http://localhost:8091 (realm: ticketing, client: ticketing-client)
// =============================================================================

const API_BASE_URL = 'http://localhost:8090/api/v1';
const ORDERS_API_URL = 'http://localhost:8090/orders';

let keycloak = null;
let currentEventPrice = 0;
let fetchedEvents = []; // Cache for local search filtering
let bookingTimeout = null; // Reference for auto-close timeout

// ---------------------------------------------------------------------------
// Utility Notifications & Error Handling
// ---------------------------------------------------------------------------
function showNotification(elementId, message, type = 'info') {
    const el = document.getElementById(elementId);
    if (!el) return;
    el.textContent = message;
    el.className = `message ${type}`;
    el.classList.remove('hidden');
    if (type === 'success') {
        setTimeout(() => el.classList.add('hidden'), 8000);
    }
}

function hideNotification(elementId) {
    const el = document.getElementById(elementId);
    if (el) el.classList.add('hidden');
}

/**
 * Extracts a friendly user-facing message from error responses.
 * Avoids showing raw technical JSON strings or Whitelabel HTML pages to clients.
 */
function getFriendlyErrorMessage(errText, defaultMsg = 'An error occurred. Please try again.') {
    if (!errText) return defaultMsg;
    
    // 1. Try to parse as JSON
    try {
        const errObj = JSON.parse(errText);
        if (errObj && typeof errObj === 'object') {
            const message = errObj.message || errObj.error || errObj.errorMessage;
            if (message && message !== 'No message available' && message !== 'Internal Server Error') {
                return message;
            }
        }
    } catch (e) {
        // Not valid JSON
    }
    
    // 2. Check if it's a raw string containing JSON-like structure
    const trimmed = errText.trim();
    if (trimmed.startsWith('{') && trimmed.endsWith('}')) {
        try {
            const msgMatch = trimmed.match(/"message"\s*:\s*"([^"]+)"/);
            if (msgMatch && msgMatch[1] && msgMatch[1] !== 'No message available') {
                return msgMatch[1];
            }
            const errMatch = trimmed.match(/"error"\s*:\s*"([^"]+)"/);
            if (errMatch && errMatch[1] && errMatch[1] !== 'Internal Server Error') {
                return errMatch[1];
            }
        } catch (e) {}
    }
    
    // 3. Check if it's an HTML Whitelabel page
    if (errText.includes('<html') || errText.includes('<body') || errText.includes('<!DOCTYPE') || errText.includes('whitelabel')) {
        return defaultMsg;
    }
    
    // 4. If it's a short text (less than 120 chars) and not JSON, return it
    if (errText.length < 120 && !errText.includes('{')) {
        return errText;
    }
    
    return defaultMsg;
}

// ---------------------------------------------------------------------------
// Utility: Button loading state
// ---------------------------------------------------------------------------
function setLoading(btn, loading, originalText) {
    if (!btn) return;
    btn.disabled = loading;
    btn.textContent = loading ? 'Processing...' : originalText;
}

// ---------------------------------------------------------------------------
// Generate Aesthetic Banner Gradients (Ticketbox Style)
// ---------------------------------------------------------------------------
function getCardGradient(id) {
    const gradients = [
        'linear-gradient(135deg, #3b82f6 0%, #1d4ed8 100%)',   // Blue
        'linear-gradient(135deg, #8b5cf6 0%, #6d28d9 100%)',   // Purple
        'linear-gradient(135deg, #10b981 0%, #047857 100%)',   // Teal
        'linear-gradient(135deg, #ec4899 0%, #be185d 100%)',   // Pink
        'linear-gradient(135deg, #f59e0b 0%, #b45309 100%)'    // Orange
    ];
    return gradients[id % gradients.length];
}

// ---------------------------------------------------------------------------
// DOM Initialization
// ---------------------------------------------------------------------------
document.addEventListener('DOMContentLoaded', async () => {

    // ── 1. Initialize Keycloak (Silent SSO check) ───────────────────────────
    try {
        keycloak = new Keycloak({
            url: 'http://localhost:8091',
            realm: 'ticketing',
            clientId: 'ticketing-client'
        });

        const authenticated = await keycloak.init({ onLoad: 'check-sso' });

        if (authenticated) {
            document.getElementById('btn-login').style.display = 'none';
            document.getElementById('user-info').classList.remove('hidden');
            
            // Show "My Orders" nav tab for logged in users
            document.getElementById('btn-nav-orders').style.display = 'inline-block';

            const token = keycloak.tokenParsed;
            const username = token.preferred_username || token.email || 'Demo User';
            document.getElementById('user-name').textContent = username;

            // Role-based admin tab — only show if user has ADMIN role
            const roles = token.realm_access?.roles || [];
            if (roles.includes('ADMIN') || roles.includes('admin')) {
                document.getElementById('btn-nav-admin').style.display = 'inline-block';
            }
        } else {
            document.getElementById('btn-login').style.display = 'inline-block';
            document.getElementById('user-info').classList.add('hidden');
        }
    } catch (e) {
        console.warn('Keycloak not reachable — running in demo mode:', e.message);
    }

    // ── 2. Auth Buttons ──────────────────────────────────────────────────────
    document.getElementById('btn-login').addEventListener('click', () => {
        if (keycloak) keycloak.login();
    });
    document.getElementById('btn-logout').addEventListener('click', () => {
        if (keycloak) keycloak.logout();
    });

    // ── 3. Fetch helper (adds Bearer token if authenticated) ─────────────────
    const fetchWithAuth = async (url, options = {}) => {
        if (keycloak && keycloak.authenticated) {
            try {
                await keycloak.updateToken(30);
            } catch (e) {
                console.warn('Token refresh failed:', e);
            }
            options.headers = {
                ...options.headers,
                'Authorization': `Bearer ${keycloak.token}`
            };
        }
        return fetch(url, options);
    };

    // ── 4. Dynamic Guest Event Listing ───────────────────────────────────────
    const eventsGrid = document.getElementById('events-grid');

    const fetchAndRenderEvents = async () => {
        try {
            const response = await fetch(`${API_BASE_URL}/inventory/events`);
            if (!response.ok) throw new Error('Could not retrieve events catalog.');
            
            fetchedEvents = await response.json();
            renderEventCards(fetchedEvents);
        } catch (error) {
            eventsGrid.innerHTML = `
                <div class="loading-state">
                    <p style="color: var(--error);">Error loading events catalog: ${error.message}</p>
                    <p style="font-size: 0.85rem; margin-top: 0.5rem;">Verify that your inventory-service and API Gateway are running.</p>
                </div>`;
        }
    };

    const renderEventCards = (events) => {
        if (!events || events.length === 0) {
            eventsGrid.innerHTML = '<p class="empty-state">No events matched your search.</p>';
            return;
        }

        eventsGrid.innerHTML = '';
        events.forEach(event => {
            const priceFormatted = event.ticketPrice
                ? Number(event.ticketPrice).toLocaleString('vi-VN') + ' ₫'
                : '10.000 ₫';
            const gradient = getCardGradient(event.eventId || 0);
            
            // Map category tags based on title/name keywords
            let category = 'LIVE SHOW';
            if (event.event.toUpperCase().includes('FEST')) category = 'FESTIVAL';
            if (event.event.toUpperCase().includes('EDM') || event.event.toUpperCase().includes('NIGHT')) category = 'EDM NIGHT';
            if (event.event.toUpperCase().includes('ROCK')) category = 'CONCERT';

            const card = document.createElement('div');
            card.className = 'event-card';
            card.innerHTML = `
                <div class="event-card-banner" style="background: ${gradient}">
                    <span class="event-category-tag">${category}</span>
                    <span class="event-stock-badge ${event.leftCapacity > 0 ? '' : 'sold-out'}">
                        ${event.leftCapacity > 0 ? event.leftCapacity + ' Available' : 'SOLD OUT'}
                    </span>
                </div>
                <div class="event-card-body">
                    <h4 class="event-card-title">${event.event}</h4>
                    <p class="event-card-venue">
                        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"></path><circle cx="12" cy="10" r="3"></circle></svg>
                        ${event.venue ? event.venue.name : 'Unknown Venue'}
                    </p>
                    <div class="capacity-progress-bar">
                        <div class="capacity-progress-fill" style="width: ${Math.min(100, Math.max(10, (event.leftCapacity / (event.totalCapacity || 100)) * 100))}%"></div>
                    </div>
                    <div class="event-card-footer">
                        <span class="event-price">${priceFormatted}</span>
                        <button class="btn-book-card" onclick="openBookingModal(${event.eventId})">Book Ticket</button>
                    </div>
                </div>
            `;
            eventsGrid.appendChild(card);
        });
    };

    // Load events on page load
    await fetchAndRenderEvents();

    // ── 5. Client-Side Instant Search Filter ────────────────────────────────
    const searchInput = document.getElementById('search-input');
    searchInput.addEventListener('input', (e) => {
        const query = e.target.value.toLowerCase().trim();
        const filtered = fetchedEvents.filter(event => 
            event.event.toLowerCase().includes(query) || 
            (event.venue && event.venue.name.toLowerCase().includes(query))
        );
        renderEventCards(filtered);
    });

    // ── 6. Tab Navigation ────────────────────────────────────────────────────
    const navButtons = document.querySelectorAll('.nav-btn');
    const tabContents = document.querySelectorAll('.tab-content');

    navButtons.forEach(btn => {
        btn.addEventListener('click', () => {
            navButtons.forEach(b => b.classList.remove('active'));
            tabContents.forEach(c => { c.classList.remove('active'); c.classList.add('hidden'); });

            // Clear any active feedbacks/notifications on tab switch
            hideNotification('booking-message');
            hideNotification('orders-feedback');
            hideNotification('admin-message');

            btn.classList.add('active');
            const target = document.getElementById(btn.dataset.target);
            target.classList.remove('hidden');
            target.classList.add('active');
        });
    });

    // ── 7. Booking Modal Logic & Helpers ─────────────────────────────────────
    const bookingForm = document.getElementById('booking-form');
    const bookingMessage = document.getElementById('booking-message');

    window.openBookingModal = async (eventId) => {
        hideNotification('booking-message');
        
        try {
            const res = await fetch(`${API_BASE_URL}/inventory/event/${eventId}`);
            if (!res.ok) throw new Error('Failed to retrieve event details.');
            
            const eventData = await res.json();
            
            currentEventPrice = eventData.ticketPrice || 0;
            document.getElementById('modal-event-title').textContent = eventData.event;
            document.getElementById('modal-event-venue').textContent = eventData.venue ? eventData.venue.name : 'Location';
            document.getElementById('modal-ticket-price').textContent = Number(currentEventPrice).toLocaleString('vi-VN') + ' ₫';
            document.getElementById('modal-event-left').textContent = `${eventData.capacity} tickets left`;
            document.getElementById('book-event-id').value = eventId;
            document.getElementById('ticket-count').value = 1;
            
            updateTotalPrice();
            
            // Show modal
            document.getElementById('booking-modal').classList.remove('hidden');
        } catch (err) {
            alert(`Error loading event: ${err.message}`);
        }
    };

    window.quickBookFeatured = (eventId) => {
        window.openBookingModal(eventId);
    };

    window.adjustQty = (delta) => {
        const input = document.getElementById('ticket-count');
        let val = parseInt(input.value) + delta;
        if (val < 1) val = 1;
        if (val > 10) val = 10;
        input.value = val;
        updateTotalPrice();
    };

    function updateTotalPrice() {
        const qty = parseInt(document.getElementById('ticket-count').value) || 1;
        const total = currentEventPrice * qty;
        document.getElementById('booking-total-price').textContent = Number(total).toLocaleString('vi-VN') + ' ₫';
    }

    window.closeModal = () => {
        document.getElementById('booking-modal').classList.add('hidden');
        bookingForm.reset();
        hideNotification('booking-message');
        if (bookingTimeout) {
            clearTimeout(bookingTimeout);
            bookingTimeout = null;
        }
    };

    bookingForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        
        // GUEST AUTHENTICATION GATE
        if (keycloak && !keycloak.authenticated) {
            if (confirm('Authentication required. Redirecting you to Keycloak to log in.')) {
                keycloak.login();
            }
            return;
        }

        const userId = parseInt(document.getElementById('user-id').value);
        const eventId = parseInt(document.getElementById('book-event-id').value);
        const ticketCount = parseInt(document.getElementById('ticket-count').value);

        if (!userId || userId < 1) {
            showNotification('booking-message', 'Please enter a valid Customer ID (e.g. 3).', 'error');
            return;
        }

        const submitBtn = document.getElementById('btn-book');
        setLoading(submitBtn, true, 'Confirm Purchase');
        hideNotification('booking-message');

        try {
            const response = await fetchWithAuth(`${API_BASE_URL}/booking`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ userId, eventId, ticketCount })
            });

            if (!response.ok) {
                const errText = await response.text();
                const friendlyMsg = getFriendlyErrorMessage(errText, 'Purchase failed. Please verify event availability.');
                throw new Error(friendlyMsg);
            }

            const data = await response.json();
            const totalFormatted = data.totalPrice
                ? Number(data.totalPrice).toLocaleString('vi-VN') + ' ₫'
                : data.totalPrice;

            showNotification('booking-message', 
                `🎉 Booking Successful! Redirecting you to orders tab to pay...`, 
                'success');

            // Refresh catalog and auto-redirect to My Orders tab to pay
            bookingTimeout = setTimeout(async () => {
                await fetchAndRenderEvents();
                closeModal();
                
                // Switch to My Orders tab
                const ordersBtn = document.getElementById('btn-nav-orders');
                if (ordersBtn) {
                    ordersBtn.click();
                    
                    // Auto-fill and fetch orders for the customer
                    const customerIdOverride = document.getElementById('user-id').value;
                    document.getElementById('history-user-id').value = customerIdOverride;
                    document.getElementById('btn-fetch-orders').click();
                }
            }, 2000);

        } catch (error) {
            showNotification('booking-message', `Error: ${error.message}`, 'error');
        } finally {
            setLoading(submitBtn, false, 'Confirm Purchase');
        }
    });

    // ── 8. My Orders (Purchase History) ──────────────────────────────────────
    const fetchOrdersBtn = document.getElementById('btn-fetch-orders');
    const ordersList = document.getElementById('orders-list');

    fetchOrdersBtn.addEventListener('click', async () => {
        const userId = document.getElementById('history-user-id').value.trim();
        if (!userId) {
            showNotification('orders-feedback', 'Enter Customer ID to check history.', 'error');
            return;
        }
        hideNotification('orders-feedback');
        setLoading(fetchOrdersBtn, true, 'Fetch Orders');
        ordersList.innerHTML = '<div class="loading-state"><div class="spinner"></div><p>Fetching purchase history...</p></div>';

        try {
            const response = await fetchWithAuth(`${ORDERS_API_URL}/history?CustomerID=${userId}`);
            if (!response.ok) {
                const errText = await response.text();
                const friendlyMsg = getFriendlyErrorMessage(errText, 'Failed to fetch purchase history. Please verify your Customer ID.');
                throw new Error(friendlyMsg);
            }

            const orders = await response.json();
            ordersList.innerHTML = '';

            if (!orders || orders.length === 0) {
                ordersList.innerHTML = '<p class="empty-state">No active bookings found for this customer ID.</p>';
                return;
            }

            orders.forEach(order => {
                const totalFormatted = order.totalPrice
                    ? Number(order.totalPrice).toLocaleString('vi-VN') + ' ₫'
                    : '-';
                const dateFormatted = order.placedAt
                    ? new Date(order.placedAt).toLocaleString('vi-VN')
                    : '-';

                // Handle payment status (assume order.status is sent from backend)
                const status = order.status || 'PENDING'; 
                const statusClass = status.toUpperCase() === 'PAID' ? 'status-paid' : 'status-pending';
                const statusLabel = status.toUpperCase() === 'PAID' ? 'Paid' : 'Unpaid';

                const card = document.createElement('div');
                card.className = 'order-item';
                card.innerHTML = `
                    <div class="order-info">
                        <div class="order-title-row">
                            <strong>Order #${order.id}</strong>
                            <span class="order-status-badge ${statusClass}">${statusLabel}</span>
                        </div>
                        <span>Event ID: ${order.eventId}</span>
                        <span>Tickets: ${order.ticketCount}</span>
                        <span>Total: ${totalFormatted}</span>
                        <span class="order-date">Date: ${dateFormatted}</span>
                    </div>
                    <div class="order-actions">
                        ${status.toUpperCase() !== 'PAID' ? `<button class="btn-pay" id="pay-btn-${order.id}" onclick="payOrder(${order.id})">Pay Now</button>` : ''}
                        <button class="btn-danger" id="cancel-btn-${order.id}" onclick="cancelOrder(${order.id})">Cancel Ticket</button>
                    </div>
                `;
                ordersList.appendChild(card);
            });

        } catch (err) {
            ordersList.innerHTML = '';
            showNotification('orders-feedback', `❌ ${err.message}`, 'error');
        } finally {
            setLoading(fetchOrdersBtn, false, 'Fetch Orders');
        }
    });

    window.cancelOrder = async (orderId) => {
        const btn = document.getElementById(`cancel-btn-${orderId}`);
        if (!confirm(`Cancel order #${orderId}? This restores the inventory capacity.`)) return;

        if (btn) { btn.disabled = true; btn.textContent = 'Cancelling...'; }

        try {
            const res = await fetchWithAuth(`${ORDERS_API_URL}/${orderId}/cancel`, { method: 'DELETE' });
            if (!res.ok) {
                const errText = await res.text();
                const friendlyMsg = getFriendlyErrorMessage(errText, 'Failed to cancel order.');
                throw new Error(friendlyMsg);
            }
            showNotification('orders-feedback', `✅ Canceled order #${orderId} successfully. Capacity restored.`, 'success');
            
            // Reload order list
            fetchOrdersBtn.click();
            // Reload event catalog capacity
            await fetchAndRenderEvents();
        } catch (err) {
            showNotification('orders-feedback', `❌ ${err.message}`, 'error');
            if (btn) { btn.disabled = false; btn.textContent = 'Cancel Ticket'; }
        }
    };

    window.payOrder = async (orderId) => {
        const btn = document.getElementById(`pay-btn-${orderId}`);
        if (btn) { btn.disabled = true; btn.textContent = 'Redirecting...'; }
        showNotification('orders-feedback', 'Connecting to Stripe secure checkout...', 'info');

        try {
            const response = await fetchWithAuth(`${ORDERS_API_URL}/${orderId}/checkout`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' }
            });

            if (!response.ok) {
                const errText = await response.text();
                const friendlyMsg = getFriendlyErrorMessage(errText, 'Failed to initialize payment session.');
                throw new Error(friendlyMsg);
            }

            const data = await response.json();
            if (data.checkoutUrl) {
                // Redirect user to Stripe payment page
                window.location.href = data.checkoutUrl;
            } else {
                throw new Error('No checkout URL returned from payment server.');
            }
        } catch (err) {
            showNotification('orders-feedback', `Payment Error: ${err.message}`, 'error');
            if (btn) { btn.disabled = false; btn.textContent = 'Pay Now'; }
        }
    };

    // ── 9. Tester Console / Simulator drawer ────────────────────────────────
    window.toggleDevDrawer = () => {
        const drawer = document.getElementById('dev-drawer');
        drawer.classList.toggle('collapsed');
    };

    window.overrideCustomerID = (id) => {
        document.getElementById('user-id').value = id;
        document.getElementById('history-user-id').value = id;
        
        // auto-fetch if we are in orders tab
        const ordersTab = document.getElementById('orders-section');
        if (ordersTab && ordersTab.classList.contains('active')) {
            fetchOrdersBtn.click();
        }
    };

    // ── 10. Admin Controller Logic ───────────────────────────────────────────
    const adminActionSelect = document.getElementById('admin-action-select');
    const adminForm = document.getElementById('admin-form');

    adminActionSelect.addEventListener('change', (e) => {
        const action = e.target.value;
        const isCreate = action === 'create';
        const isDelete = action === 'delete';

        document.getElementById('admin-event-id-group').style.display   = isCreate ? 'none' : 'block';
        document.getElementById('admin-venue-id-group').style.display   = isCreate ? 'block' : 'none';
        document.querySelector('.name-group').style.display             = isDelete ? 'none' : 'block';
        document.querySelector('.capacity-group').style.display         = isDelete ? 'none' : 'block';
        document.querySelector('.price-group').style.display            = isDelete ? 'none' : 'block';

        const reqFields = ['admin-venue-id', 'admin-event-name', 'admin-event-total-cap', 'admin-event-left-cap', 'admin-event-price'];
        reqFields.forEach(id => {
            const el = document.getElementById(id);
            if (el) el.required = !isDelete;
        });
        if (!isCreate) {
            const venueIdEl = document.getElementById('admin-venue-id');
            if (venueIdEl) venueIdEl.required = false;
        }
    });

    adminForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        const action  = adminActionSelect.value;
        const eventId = document.getElementById('admin-event-id').value;
        const venueId = document.getElementById('admin-venue-id').value;
        const submitBtn = document.getElementById('btn-admin-submit');

        const payload = {
            id:            eventId ? parseInt(eventId) : null,
            name:          document.getElementById('admin-event-name').value,
            totalCapacity: parseInt(document.getElementById('admin-event-total-cap').value),
            leftCapacity:  parseInt(document.getElementById('admin-event-left-cap').value),
            price:         parseFloat(document.getElementById('admin-event-price').value)
        };

        let url = `${API_BASE_URL}/inventory/event`;
        let method = 'POST';

        if (action === 'create') {
            url += `/create/venue/${venueId}`;
        } else if (action === 'update') {
            url += `/${eventId}`;
            method = 'PUT';
        } else if (action === 'delete') {
            url += `/${eventId}`;
            method = 'DELETE';
        }

        hideNotification('admin-message');
        setLoading(submitBtn, true, 'Execute Action');

        try {
            const res = await fetchWithAuth(url, {
                method,
                headers: { 'Content-Type': 'application/json' },
                body: method !== 'DELETE' ? JSON.stringify(payload) : undefined
            });

            if (!res.ok) {
                const errText = await res.text();
                const friendlyMsg = getFriendlyErrorMessage(errText, `Failed to ${action} event. Please check parameters.`);
                throw new Error(friendlyMsg);
            }

            showNotification('admin-message', `Event ${action}d successfully.`, 'success');
            adminForm.reset();
            adminActionSelect.dispatchEvent(new Event('change'));
            
            // Reload grid catalog
            await fetchAndRenderEvents();

        } catch (err) {
            showNotification('admin-message', `Error: ${err.message}`, 'error');
        } finally {
            setLoading(submitBtn, false, 'Execute Action');
        }
    });

    // Trigger initial admin layout
    adminActionSelect.dispatchEvent(new Event('change'));
});
