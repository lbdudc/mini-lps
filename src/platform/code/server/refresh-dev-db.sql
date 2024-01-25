SELECT pg_terminate_backend(pg_stat_activity.pid)
FROM pg_stat_activity
WHERE pg_stat_activity.datname = '/*%= getDatabaseConfigFromSpec(data, 'database', 'gema') %*/-dev'
AND pid <> pg_backend_pid();

DROP DATABASE IF EXISTS "/*%= getDatabaseConfigFromSpec(data, 'database', 'gema') %*/-dev";

SELECT pg_terminate_backend(pg_stat_activity.pid)
FROM pg_stat_activity
WHERE pg_stat_activity.datname = '/*%= getDatabaseConfigFromSpec(data, 'database', 'gema') %*/'
AND pid <> pg_backend_pid();

CREATE DATABASE "/*%= getDatabaseConfigFromSpec(data, 'database', 'gema') %*/-dev"
WITH TEMPLATE /*%= getDatabaseConfigFromSpec(data, 'database', 'gema') %*/
OWNER /*%= getDatabaseConfigFromSpec(data, 'username', 'postgres') %*/;
