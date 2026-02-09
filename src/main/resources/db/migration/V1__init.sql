-- V1: 初始化 Postgres + PostGIS（最小骨架）

CREATE EXTENSION IF NOT EXISTS postgis;

-- 用户
CREATE TABLE IF NOT EXISTS "user" (
  id BIGSERIAL PRIMARY KEY,
  email VARCHAR(255) UNIQUE,
  display_name VARCHAR(100),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- 设备
CREATE TABLE IF NOT EXISTS device (
  id BIGSERIAL PRIMARY KEY,
  serial_no VARCHAR(64) NOT NULL UNIQUE,
  name VARCHAR(100),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- 用户-设备关联
CREATE TABLE IF NOT EXISTS user_device (
  user_id BIGINT NOT NULL,
  device_id BIGINT NOT NULL,
  role VARCHAR(32) NOT NULL DEFAULT 'owner',
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY (user_id, device_id),
  CONSTRAINT fk_user_device_user FOREIGN KEY (user_id) REFERENCES "user"(id) ON DELETE CASCADE,
  CONSTRAINT fk_user_device_device FOREIGN KEY (device_id) REFERENCES device(id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_user_device_device_id ON user_device(device_id);

-- 地理围栏
CREATE TABLE IF NOT EXISTS geofence (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL,
  name VARCHAR(100) NOT NULL,
  geom geometry(Geometry, 4326) NOT NULL,
  enabled BOOLEAN NOT NULL DEFAULT true,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT fk_geofence_user FOREIGN KEY (user_id) REFERENCES "user"(id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_geofence_user_id ON geofence(user_id);
CREATE INDEX IF NOT EXISTS idx_geofence_geom_gist ON geofence USING GIST (geom);

-- 围栏状态（按设备在某围栏内/外）
CREATE TABLE IF NOT EXISTS geofence_state (
  id BIGSERIAL PRIMARY KEY,
  geofence_id BIGINT NOT NULL,
  device_id BIGINT NOT NULL,
  inside BOOLEAN NOT NULL DEFAULT false,
  last_change_at TIMESTAMPTZ,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT fk_geofence_state_geofence FOREIGN KEY (geofence_id) REFERENCES geofence(id) ON DELETE CASCADE,
  CONSTRAINT fk_geofence_state_device FOREIGN KEY (device_id) REFERENCES device(id) ON DELETE CASCADE,
  CONSTRAINT uq_geofence_state UNIQUE (geofence_id, device_id)
);
CREATE INDEX IF NOT EXISTS idx_geofence_state_device_id ON geofence_state(device_id);

-- 围栏事件（进入/离开等）
CREATE TABLE IF NOT EXISTS geofence_event (
  id BIGSERIAL PRIMARY KEY,
  geofence_id BIGINT NOT NULL,
  device_id BIGINT NOT NULL,
  event_type VARCHAR(32) NOT NULL,
  ts TIMESTAMPTZ NOT NULL,
  location geometry(Point, 4326),
  CONSTRAINT fk_geofence_event_geofence FOREIGN KEY (geofence_id) REFERENCES geofence(id) ON DELETE CASCADE,
  CONSTRAINT fk_geofence_event_device FOREIGN KEY (device_id) REFERENCES device(id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_geofence_event_device_ts ON geofence_event(device_id, ts);
CREATE INDEX IF NOT EXISTS idx_geofence_event_geofence_ts ON geofence_event(geofence_id, ts);

-- 位置历史（轨迹）
CREATE TABLE IF NOT EXISTS location_history (
  id BIGSERIAL PRIMARY KEY,
  device_id BIGINT NOT NULL,
  ts TIMESTAMPTZ NOT NULL,
  location geometry(Point, 4326) NOT NULL,
  accuracy_m DOUBLE PRECISION,
  speed_mps DOUBLE PRECISION,
  CONSTRAINT fk_location_history_device FOREIGN KEY (device_id) REFERENCES device(id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_location_history_device_ts ON location_history(device_id, ts);

-- 刷新令牌
CREATE TABLE IF NOT EXISTS refresh_token (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL,
  token_hash VARCHAR(255) NOT NULL UNIQUE,
  expires_at TIMESTAMPTZ NOT NULL,
  revoked_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT fk_refresh_token_user FOREIGN KEY (user_id) REFERENCES "user"(id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_refresh_token_user_id ON refresh_token(user_id);
