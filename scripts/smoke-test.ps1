$ErrorActionPreference = "Stop"

$gatewayUrl = $env:EVENTTICK_GATEWAY_URL
if (-not $gatewayUrl) {
    $gatewayUrl = "http://localhost:8090"
}

$keycloakUrl = $env:EVENTTICK_KEYCLOAK_URL
if (-not $keycloakUrl) {
    $keycloakUrl = "http://localhost:8091"
}

$realm = $env:EVENTTICK_KEYCLOAK_REALM
if (-not $realm) {
    $realm = "ticketing"
}

$clientId = $env:EVENTTICK_KEYCLOAK_CLIENT_ID
if (-not $clientId) {
    $clientId = "ticketing-client"
}

function Wait-HttpOk {
    param(
        [Parameter(Mandatory = $true)] [string] $Url,
        [int] $Attempts = 30,
        [int] $DelaySeconds = 5
    )

    for ($i = 1; $i -le $Attempts; $i++) {
        try {
            Invoke-RestMethod -Uri $Url -Method Get -TimeoutSec 5 | Out-Null
            return
        }
        catch {
            if ($i -eq $Attempts) {
                throw
            }
            Start-Sleep -Seconds $DelaySeconds
        }
    }
}

Write-Host "Waiting for Keycloak..."
Wait-HttpOk "$keycloakUrl/realms/$realm"

Write-Host "Waiting for API Gateway..."
Wait-HttpOk "$gatewayUrl/actuator/health"

Write-Host "Requesting customer token..."
$tokenResponse = Invoke-RestMethod `
    -Uri "$keycloakUrl/realms/$realm/protocol/openid-connect/token" `
    -Method Post `
    -ContentType "application/x-www-form-urlencoded" `
    -Body @{
        grant_type = "password"
        client_id  = $clientId
        username   = "customer"
        password   = "customer123"
    }

$headers = @{
    Authorization = "Bearer $($tokenResponse.access_token)"
}

Write-Host "Fetching demo event..."
$event = Invoke-RestMethod -Uri "$gatewayUrl/api/v1/inventory/event/1" -Headers $headers
Write-Host "Event #1: $($event.event), available tickets: $($event.capacity)"

Write-Host "Creating demo booking..."
$booking = Invoke-RestMethod `
    -Uri "$gatewayUrl/api/v1/booking" `
    -Method Post `
    -Headers $headers `
    -ContentType "application/json" `
    -Body (@{
        userId = 1
        eventId = 1
        ticketCount = 1
    } | ConvertTo-Json)

Write-Host "Booking accepted. Total: $($booking.totalPrice)"
Write-Host "Waiting for Order Service to consume Kafka event..."
Start-Sleep -Seconds 5

$orders = Invoke-RestMethod -Uri "$gatewayUrl/orders/history?CustomerID=1" -Headers $headers
if (-not $orders -or $orders.Count -lt 1) {
    throw "Booking was accepted but no order was found for CustomerID=1"
}

Write-Host "Smoke test passed. Orders found: $($orders.Count)"
