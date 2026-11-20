/*
 * Versão de Homologação/Testes
 */
package br.gov.serpro.rtc.config.security;

import static org.springframework.security.config.Customizer.withDefaults;
import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer.FrameOptionsConfig;
import org.springframework.security.web.SecurityFilterChain;

import lombok.RequiredArgsConstructor;

/**
 * Configura a cadeia de segurança HTTP da aplicação com sessão stateless e
 * acesso liberado às rotas.
 */
@Configuration
@RequiredArgsConstructor(onConstructor_ = { @Autowired })
@EnableWebSecurity
public class WebSecurityConfig {

    @Bean
    SecurityFilterChain localFilterChain(HttpSecurity http) throws Exception {
        return http
            .securityMatcher("/**")
            // necessario para colocar o Swagger dentro de um frame no frontend do portal da RTC
            .headers(headers -> headers.frameOptions(FrameOptionsConfig::sameOrigin))
            .cors(withDefaults())
            .formLogin(AbstractHttpConfigurer::disable)
            .logout(AbstractHttpConfigurer::disable)
            .csrf(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .anonymous(AbstractHttpConfigurer::disable)
            .sessionManagement(configurer -> configurer.sessionCreationPolicy(STATELESS))
            .authorizeHttpRequests(configurer -> configurer.anyRequest().permitAll())
            .build();
    }

}