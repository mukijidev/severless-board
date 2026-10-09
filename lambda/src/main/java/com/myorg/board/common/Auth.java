package com.myorg.board.common;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;

import java.util.Map;

public final class Auth {
    private Auth() {}

    public static String userId(APIGatewayProxyRequestEvent event) {

        APIGatewayProxyRequestEvent.ProxyRequestContext context =
                event.getRequestContext();

        if(context == null || context.getAuthorizer() == null)
        {
            return null;
        }

        Object claims = context.getAuthorizer().get("claims");
        if(claims instanceof  Map<?, ?> map)
        {
            Object sub = map.get("sub");
            return sub == null ? null : sub.toString();
        }
        return null;
    }
}

