package com.jazara.icu.monitor;

import de.codecentric.boot.admin.server.config.AdminServerProperties;
import de.codecentric.boot.admin.server.config.EnableAdminServer;
import io.netty.handler.codec.http.HttpMethod;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.netflix.turbine.stream.EnableTurbineStream;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import com.ulisesbocchio.jasyptspringboot.annotation.EnableEncryptableProperties;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import java.util.UUID;

@SpringBootApplication
@EnableDiscoveryClient
@EnableAdminServer
@EnableTurbineStream
@EnableEncryptableProperties
public class MonitorApplication {

    public static void main(String[] args) {
        SpringApplication.run(MonitorApplication.class, args);
    }

    @Configuration
    public static class WebSecurityConfig extends WebSecurityConfigurerAdapter {
        private final AdminServerProperties adminServer;

        public WebSecurityConfig(AdminServerProperties adminServer) {
            this.adminServer = adminServer;
        }

        @Override
        protected void configure(HttpSecurity http) throws Exception {
            SavedRequestAwareAuthenticationSuccessHandler successHandler =
                    new SavedRequestAwareAuthenticationSuccessHandler();
            successHandler.setTargetUrlParameter("redirectTo");
            successHandler.setDefaultTargetUrl(this.adminServer.getContextPath() + "/");

            http
                    .authorizeRequests()
                    .antMatchers(this.adminServer.getContextPath() + "/assets/**").permitAll()
                    .antMatchers(this.adminServer.getContextPath() + "/login").permitAll()
                    .anyRequest().authenticated()
                    .and()
                    .formLogin()
                    .loginPage(this.adminServer.getContextPath() + "/login")
                    .successHandler(successHandler)
                    .and()
                    .logout()
                    .logoutUrl(this.adminServer.getContextPath() + "/logout")
                    .and()
                    .httpBasic()
                    .and()
                    .csrf()
                    .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                    .ignoringRequestMatchers(
                            new AntPathRequestMatcher(this.adminServer.getContextPath() +
                                    "/instances", HttpMethod.POST.toString()),
                            new AntPathRequestMatcher(this.adminServer.getContextPath() +
                                    "/instances/*", HttpMethod.DELETE.toString()),
                            new AntPathRequestMatcher(this.adminServer.getContextPath() + "/actuator/**"))
                    .and()
                    .rememberMe()
                    .key(UUID.randomUUID().toString())
                    .tokenValiditySeconds(1209600);
        }

        // Required to provide UserDetailsService for "remember functionality"
        @Override
        protected void configure(AuthenticationManagerBuilder auth) throws Exception {
            auth.inMemoryAuthentication().withUser("admin").password("admin").roles("ADMIN");
        }

    }
}