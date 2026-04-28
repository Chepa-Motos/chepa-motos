# ChepaMotos Bruno Collection

This folder contains a Bruno-native collection for the ChepaMotos API.

## Contents

- `bruno.json` - collection metadata
- `collection.bru` - collection root
- `environments/Local.bru` - local environment variables
- `Local.json` - JSON copy for Bruno's environment import dialog
- `*.bru` files - request files, ordered to support the main test flow

## How to use

1. Open Bruno.
2. Open this folder as a collection.
3. Import `Local.json` from this folder as the environment file (only do this if the Local environment doesnt show up when importing the collection).
4. Select the imported `Local` environment.
5. Make sure the API is running at `http://localhost:8080`.
6. Run the requests in sequence so the dependent variables are populated.

If you already opened the collection before importing the environment, that also works. The important part is to import the JSON environment file, not `environments/Local.bru`, because Bruno's environment import dialog expects JSON.

## Recommended request order

1. Auth Login
2. Create Mechanic
3. Get Mechanic By Id
4. List Mechanics
5. Create Service Invoice
6. Lookup Vehicle By Plate
7. Create Delivery Invoice
8. List Invoices
9. Get Invoice By Id
10. Cancel Invoice
11. Invoice Item Suggestions
12. Create Liquidation
13. List Liquidations
14. Change Mechanic Status
15. Auth Refresh
16. Auth Logout

## Environment values

The local environment ships with example bootstrap values:

- `host`: `http://localhost:8080`
- `adminUsername`: `gerente`
- `adminPassword`: `password`

If your root `.env` already defines `ADMIN_USERNAME` and `ADMIN_PASSWORD`, use those values here.

If your local backend uses different bootstrap credentials, update `environments/Local.bru` before running the collection.

## Notes

- The login request stores `authToken` and `refreshToken` for later requests.
- The mechanic creation request stores `mechanicId`.
- The service invoice request stores `invoiceId`.
- Manager-only endpoints require the bearer token returned by login.
- If you are using Bruno's environment import dialog, import `Local.json` instead of `environments/Local.bru`.
- `authToken` and `refreshToken` are created automatically by the login request; do not set them manually.
- Do not commit real secrets or production credentials to GitHub.
