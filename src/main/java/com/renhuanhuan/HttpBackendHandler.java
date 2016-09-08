package com.renhuanhuan;

import io.netty.channel.*;
import io.netty.handler.codec.http.FullHttpResponse;

/**
 * Created by rahul on 9/8/16.
 */
public class HttpBackendHandler extends SimpleChannelInboundHandler<FullHttpResponse> {

    private final Channel inboundChannel;

    public HttpBackendHandler(Channel inboundChannel) {
        this.inboundChannel = inboundChannel;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("Backend Handler is Active!");
        super.channelActive(ctx);
    }

    @Override
    public void channelRead0(final ChannelHandlerContext ctx, FullHttpResponse msg) throws Exception {
        inboundChannel.writeAndFlush(msg.retain()).addListener(new ChannelFutureListener() {
            public void operationComplete(ChannelFuture future) throws Exception {
                if (!future.isSuccess()) {
                    future.channel().close();
                }
            }
        });
    }
    
    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        System.out.println("Backend Handler destroyed!");
        HttpFrontendHandler.closeOnFlush(inboundChannel);
    }
    
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        HttpFrontendHandler.closeOnFlush(ctx.channel());
    }
}
