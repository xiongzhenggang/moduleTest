package com.xzg.security.service.config;

import com.xzg.security.service.Utiils.BCryptUtil;
import com.xzg.security.service.selfToken.JwtAccessToken;
import com.xzg.security.service.service.BaseUserDetailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.config.annotation.configurers.ClientDetailsServiceConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configuration.AuthorizationServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableAuthorizationServer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerEndpointsConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerSecurityConfigurer;
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;
import org.springframework.security.oauth2.provider.token.store.KeyStoreKeyFactory;

/**
 * @author xzg
 * 授权认证服务：AuthenticationServer
 */
@Configuration
public class OAuth2Config extends AuthorizationServerConfigurerAdapter {
    @Autowired
    private BaseUserDetailService userDetailsService;

    @Autowired
    private AuthenticationManager authenticationManager;

    /**
     * @param endpointsConfigurer
     * 用来配置授权（authorization）以及令牌（token）的访问端点和令牌服务(token services)。
     * @throws Exception
     * 配置令牌 管理 (jwtAccessTokenConverter)
     */
    @Override
    public void configure(AuthorizationServerEndpointsConfigurer endpoints) {
        endpoints.authenticationManager(authenticationManager)
                // 配置JwtAccessToken转换器
                .accessTokenConverter(jwtAccessTokenConverter())
                // refresh_token需要userDetailsService
                //走password的就是用AuthorizationServerEndpointsConfigurer中配置的userDetailsService来进行认证
                .reuseRefreshTokens(false)
                .userDetailsService(userDetailsService);
        //.tokenStore(getJdbcTokenStore());
    }

    /**
     * @param clientDetailsServiceConfigurer
     * ClientDetailsServiceConfigurer：用来配置客户端详情服务（ClientDetailsService）
     * 客户端详情信息在这里进行初始化，你能够把客户端详情信息写死在这里或者是通过数据库来存储调取详情信息。
     * @throws Exception
     */
    @Override
    public void configure(ClientDetailsServiceConfigurer clientDetailsServiceConfigurer) throws Exception {
        // Using hardcoded inmemory mechanism because it is just an example
        clientDetailsServiceConfigurer
                .inMemory()//使用方法代替in-memory、JdbcClientDetailsService、jwt
//        client_id: 用来标识客户的Id。第三方用户的id（可理解为账号）
                .withClient("client")
//        client_secret:第三方应用和授权服务器之间的安全凭证(可理解为密码)
                //（需要值得信任的客户端）客户端安全码
                .secret(BCryptUtil.encodePassword("password"))
                .accessTokenValiditySeconds(7200)
                .authorizedGrantTypes("authorization_code", "refresh_token", "client_credentials", "implicit", "password")
//                .scopes("app");
                .authorities("ROLE_USER")
                .scopes("apiAccess");
    }


    /**
     * @param security
     *  AuthorizationServerSecurityConfigurer：用来配置令牌端点(Token Endpoint)的安全约束
     * @throws Exception
     */
    @Override
    public void configure(AuthorizationServerSecurityConfigurer security) throws Exception {
        security
                // 开启/oauth/token_key验证端口无权限访问
                .tokenKeyAccess("permitAll()")
                // 开启/oauth/check_token验证端口认证权限访问
//                .checkTokenAccess("isAuthenticated()")
                .checkTokenAccess("permitAll()")
                .passwordEncoder(new BCryptPasswordEncoder())
//        请求/oauth/token的，如果配置支持allowFormAuthenticationForClients的，且url中有client_id和client_secret的会走ClientCredentialsTokenEndpointFilter
                .allowFormAuthenticationForClients();
    }
    /**
     * 使用非对称加密算法来对Token进行签名
     * @return
     */
    @Bean
    public JwtAccessTokenConverter jwtAccessTokenConverter() {

        final JwtAccessTokenConverter converter = new JwtAccessToken();
        // 导入证书
        KeyStoreKeyFactory keyStoreKeyFactory =
                new KeyStoreKeyFactory(new ClassPathResource("keystore.jks"), "password".toCharArray());

        converter.setKeyPair(keyStoreKeyFactory.getKeyPair("selfsigned"));

        return converter;
    }
}