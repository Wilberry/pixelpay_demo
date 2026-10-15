# PixelPay — Render and Supabase deployment

## Architecture

The Spring Boot Java 17 API is packaged by the repository Dockerfile and hosted as a Render web service in Frankfurt. PostgreSQL is hosted by the dedicated Supabase project `pixelpay` in Frankfurt. This deployment is a **demonstration**, not a licensed financial service; do not use real funds or personal financial information.

## Supabase database

Project ref: `pklxjhcjibuavyrpqodu`. The `initial_pixelwallet_jpa_schema` migration creates `users`, `wallets`, and `transactions` with constraints and indexes. Do not use Supabase publishable/anon keys as JDBC passwords.

## Required Render environment variables

- `DATABASE_URL`: PostgreSQL JDBC URL, e.g. `jdbc:postgresql://<HOST>:5432/postgres?sslmode=require`. Obtain the **actual** connection host and port from Supabase Dashboard → Connect. Depending on IPv4 support, use Supabase's session pooler with its displayed host, port and user.
- `DATABASE_USERNAME`: PostgreSQL username shown in the same Supabase connection panel (pooler usernames can differ from `postgres`).
- `DATABASE_PASSWORD`: Database password from Supabase, entered **only** in Render's secret environment variable settings.
- `JWT_SECRET`: A cryptographically random secret of **at least 64 bytes** for the current HS512 JWT signer; never commit or paste it into chat.
- `HIBERNATE_DDL_AUTO=validate`: Verify the pre-existing migration rather than creating schema on startup.
- `PIXELWALLET_DEMO_ENABLED=false`: No automatic wallet credits. For an isolated, clearly labeled simulated-money demonstration only, change to `true` intentionally.

`PORT` is provided by Render. Set the web service plan to **Free**, runtime **Docker**, region **Frankfurt**, branch `main`. The Dockerfile builds the Maven package with tests skipped, so CI tests and end-to-end checks are separately required before treating deployment as verified.

## Security and release checks

1. Verify the Maven build and tests, then Hibernate schema validation.
2. Ensure the database credentials are supplied via Render secrets and the database connection uses TLS.
3. Verify anonymous access is denied for wallet and transaction APIs.
4. Verify registration/login, wallet creation, transfers, insufficient funds, duplicate reference numbers, and concurrent transfers with **simulated balances only**.
5. Confirm no actual banking integrations or real-money claims are presented.
6. Monitor startup logs and database connection errors before enabling auto-deploy.

The Supabase connector does not expose the project's database password. Configure secrets through the provider dashboards, not GitHub or this chat.
