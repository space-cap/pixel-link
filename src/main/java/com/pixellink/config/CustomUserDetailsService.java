package com.pixellink.config;

import com.pixellink.mapper.UserMapper;
import com.pixellink.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserMapper userMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userMapper.findById(username);
        if (user == null || user.getPassword() == null) {
            throw new UsernameNotFoundException("아이디 혹은 비밀번호가 틀렸습니다.");
        }
        return new CustomUserDetails(user);
    }
}
