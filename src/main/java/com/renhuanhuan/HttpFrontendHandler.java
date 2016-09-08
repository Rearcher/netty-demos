package com.renhuanhuan;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.http.FullHttpRequest;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by rahul on 9/8/16.
 */
@SuppressWarnings("Duplicates")
public class HttpFrontendHandler extends SimpleChannelInboundHandler<FullHttpRequest>{

    private Channel outboundChannel;

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("FrontEnd Handler is Active!");
        super.channelActive(ctx);
    }

    @Override
    public void channelRead0(final ChannelHandlerContext ctx, final FullHttpRequest msg) throws Exception {
        final Channel inboundChannel = ctx.channel();

        String host = msg.headers().get("Host");
        int port = 80;

        String pattern = "(http://|https://)?([^:]+)(:[\\d]+)?";
        Pattern r = Pattern.compile(pattern);
        Matcher m = r.matcher(host);
        if (m.find()) {
            host = m.group(2);
            port = (m.group(3) == null) ? 80 : Integer.parseInt(m.group(3).substring(1));
        }

        Bootstrap b = new Bootstrap();
        b.group(inboundChannel.eventLoop())     // use inboundChannel thread
                .channel(ctx.channel().getClass())
                .handler(new BackendHandlerInitializer(inboundChannel));

        ChannelFuture f = b.connect(host, port);
        outboundChannel = f.channel();
        msg.retain();
        ChannelFuture channelFuture = f.addListener(new ChannelFutureListener() {
            public void operationComplete(ChannelFuture future) throws Exception {
                if (future.isSuccess()) {
                    outboundChannel.writeAndFlush(msg);
                } else {
                    inboundChannel.close();
                }
            }
        });
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        System.out.println("Frontend Handler destroyed!");
        if (outboundChannel != null) {
            closeOnFlush(outboundChannel);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        closeOnFlush(ctx.channel());
    }

    static void closeOnFlush(Channel ch) {
        if (ch.isActive()) {
            ch.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
        }
    }
}
