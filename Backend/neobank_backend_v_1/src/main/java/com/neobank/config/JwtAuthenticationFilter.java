package com.neobank.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.neobank.service.AuthService;

import java.io.IOException;
import java.util.Collections;

//ADD
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	
	private final JwtUtil jwtUtil;

//	@Override
//	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
//			throws ServletException, IOException {
//
//		String header = request.getHeader("Authorization");
//
//		if (header != null && header.startsWith("Bearer ")) {
//			String token = header.substring(7);
//			if (jwtUtil.isTokenValid(token)) {
//				String email = jwtUtil.extractEmail(token);
////    var auth = new UsernamePasswordAuthenticationToken(
////            email, null, Collections.emptyList());
//
//				// CHANGE//////////////////////////
//				String role = jwtUtil.extractRole(token);
//
//				// Spring requires ROLE_ prefix
//				var auth = new UsernamePasswordAuthenticationToken(email, null,
////            List.of(new SimpleGrantedAuthority("ROLE_" + role))
//						Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role)));
//				////////////////////////////////////
//
//				SecurityContextHolder.getContext().setAuthentication(auth);
//			}
//		}
//
//		chain.doFilter(request, response);
//	}
	
	@Override
	protected void doFilterInternal(HttpServletRequest request,
	                                HttpServletResponse response,
	                                FilterChain chain)
	        throws ServletException, IOException {

	    String header = request.getHeader("Authorization");

	    // ── ADD TEMP LOG ──
	    log.info("JWT Filter hit: {} {} | Auth header: {}",
	        request.getMethod(),
	        request.getRequestURI(),
	        header != null ? "present" : "MISSING"   // don't log the token itself
	    );

	    if (header != null && header.startsWith("Bearer ")) {
	        String token = header.substring(7);
	        if (jwtUtil.isTokenValid(token)) {
	            String username = jwtUtil.extractUsername(token);
	            String role     = jwtUtil.extractRole(token);

	            // ── ADD TEMP LOG ──
	            log.info("Token valid for user: {} role: {}", username, role);

	            var auth = new UsernamePasswordAuthenticationToken(
	                username, null,
	                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role))
	            );
	            SecurityContextHolder.getContext().setAuthentication(auth);
	        } else {
	            // ── ADD TEMP LOG ──
	            log.warn("Token present but INVALID for URI: {}", request.getRequestURI());
	        }
	    }
	    chain.doFilter(request, response);
	}
}






