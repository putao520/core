package common.java.interfaceType;

import java.lang.annotation.Repeatable;


@Repeatable(_ApiType.class)
public @interface ApiType {
    type value() default type.PublicApi;

    enum type {PublicApi, SessionApi, OauthApi, CloseApi, PrivateApi}
}