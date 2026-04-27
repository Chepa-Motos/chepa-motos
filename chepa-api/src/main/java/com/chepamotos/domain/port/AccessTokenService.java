package com.chepamotos.domain.port;

import com.chepamotos.domain.model.AccessToken;
import com.chepamotos.domain.model.AppUser;

public interface AccessTokenService {

    AccessToken generate(AppUser user);
}
