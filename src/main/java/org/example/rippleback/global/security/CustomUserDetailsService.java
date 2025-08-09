package org.example.rippleback.global.security;

import lombok.RequiredArgsConstructor;
import org.example.rippleback.domain.user.entity.User;
import org.example.rippleback.domain.user.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * username 파라미터에 이메일이 들어옵니다.
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new UsernameNotFoundException("User not found with email: " + email));

        // 필요 시 추가 검증 (예: 탈퇴/정지 여부)
        // if (user.isDeleted() || user.isBlocked()) { throw new DisabledException("..."); }

        return new CustomUserDetails(user);
    }
}