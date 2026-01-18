package dev.yunseong.website.ai.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import dev.yunseong.apilimitmvc.domain.Factor;

public class SingleFactor implements Factor<String> {

    @Override
    public String getKey(HttpServletRequest request, HttpServletResponse response) {
        return "single_factor_key";
    }
}
