package com.acme.collar.auth;

import com.fasterxml.jackson.annotation.JsonProperty;

record AuthResponse(@JsonProperty("accessToken") String accessToken) {}
