package org.example.murderhelp.global.filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.example.murderhelp.domain.member.entity.Member;
import org.example.murderhelp.domain.member.repository.MemberRepository;
import org.example.murderhelp.global.jwt.JwtProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
    private final MemberRepository memberRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);

            if (jwtProvider.validate(token) && !jwtProvider.isRefreshToken(token)) {
                Long memberId = jwtProvider.getMemberId(token);
                Optional<Member> memberOpt = memberRepository.findById(memberId);

                if (memberOpt.isPresent()) {
                    Member member = memberOpt.get();
                    List<GrantedAuthority> authorities =
                            List.of(new SimpleGrantedAuthority("ROLE_" + member.getGrade().name()));

                    UsernamePasswordAuthenticationToken auth =
                            new UsernamePasswordAuthenticationToken(memberId, null, authorities);
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            }
        }

        filterChain.doFilter(request, response);
    }
}