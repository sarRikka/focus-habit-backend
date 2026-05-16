package com.atomic.focus.modules.auth.service;

import com.atomic.focus.modules.auth.dto.GuestLoginDTO;
import com.atomic.focus.modules.auth.dto.PhoneLoginDTO;
import com.atomic.focus.modules.auth.dto.RefreshTokenDTO;
import com.atomic.focus.modules.auth.dto.SendCodeDTO;
import com.atomic.focus.modules.auth.vo.AuthVO;

public interface AuthService {

    AuthVO guestLogin(GuestLoginDTO dto);

    AuthVO phoneLogin(PhoneLoginDTO dto);

    AuthVO register(PhoneLoginDTO dto);

    void sendCode(SendCodeDTO dto);

    AuthVO refresh(RefreshTokenDTO dto);

    void logout();
}
