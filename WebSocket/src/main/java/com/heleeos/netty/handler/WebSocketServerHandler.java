package com.heleeos.netty.handler;

import com.heleeos.netty.function.MessageHandler;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.util.CharsetUtil;
import org.apache.log4j.Logger;

/**
 *
 * Created by liyu on 01/03/2018.
 */
public class WebSocketServerHandler extends SimpleChannelInboundHandler<Object> {

    private Logger logger = Logger.getLogger(getClass());

    private WebSocketServerHandshaker handshaker;

    /**
     * 消息处理器
     */
    private MessageHandler messageHandler;

    public WebSocketServerHandler(MessageHandler messageHandler) {
        this.messageHandler = messageHandler;
    }

    @Override
    public void messageReceived(ChannelHandlerContext context, Object msg) throws Exception {
        logger.info("接受消息");
        if (msg instanceof FullHttpRequest){
            //HTTP接入，WebSocket第一次连接使用HTTP连接, 用于握手
            handleHttpRequest(context, (FullHttpRequest) msg);
        } else if(msg instanceof WebSocketFrame){
            // WebSocket 接入
            handlerWebSocketFrame(context, (WebSocketFrame) msg);
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext context) throws Exception {
        context.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext context, Throwable cause) throws Exception {
        logger.error("产生异常，连接被关闭", cause);
        context.close();
    }

    /**
     * HTTP 请求处理
     */
    private void handleHttpRequest(ChannelHandlerContext context, FullHttpRequest request) {
        if (!request.getDecoderResult().isSuccess() || (!"websocket".equals(request.headers().get("Upgrade")))) {
            sendHttpResponse(context, request, new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_REQUEST));
            return;
        }
        WebSocketServerHandshakerFactory wsFactory = new WebSocketServerHandshakerFactory("ws://localhost:2048/ws", null, false);
        handshaker = wsFactory.newHandshaker(request);
        if (handshaker == null) {
            WebSocketServerHandshakerFactory.sendUnsupportedWebSocketVersionResponse(context.channel());
        } else {
            handshaker.handshake(context.channel(), request);
        }
    }

    private void sendHttpResponse(ChannelHandlerContext context, FullHttpRequest request, DefaultFullHttpResponse response) {
        // 返回应答给客户端
        if (response.getStatus().code() != 200) {
            ByteBuf buf = Unpooled.copiedBuffer(response.getStatus().toString(), CharsetUtil.UTF_8);
            request.content().writeBytes(buf);
            buf.release();
        }
        // 如果是非Keep-Alive，关闭连接
        ChannelFuture future = context.channel().writeAndFlush(request);
        if (!HttpHeaders.isKeepAlive(request) || response.getStatus().code() != 200) {
            future.addListener(ChannelFutureListener.CLOSE);
        }
    }

    /**
     * WebSocket请求
     */
    private void handlerWebSocketFrame(ChannelHandlerContext context, WebSocketFrame frame) {
        logger.info("WebSocket接收到信息");

        //判断是否关闭链路的指令
        if (frame instanceof CloseWebSocketFrame) {
            handshaker.close(context.channel(), (CloseWebSocketFrame) frame.retain());
            return;
        }

        //判断是否ping消息
        if (frame instanceof PingWebSocketFrame) {
            context.channel().write( new PongWebSocketFrame(frame.content().retain()));
            return;
        }

        if(frame instanceof TextWebSocketFrame) {
            if(messageHandler != null) {
                messageHandler.handlerTextWebSocketFrame(context, (TextWebSocketFrame) frame);
                return;
            }
        }

        throw new UnsupportedOperationException(String.format("不支持类型[%s]消息", frame.getClass().getName()));
    }
}
