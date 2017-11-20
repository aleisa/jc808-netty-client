package com.eminem.handler;


import com.eminem.common.MsgDecoder;
import com.eminem.common.TPMSConsts;
import com.eminem.service.SessionManager;
import com.eminem.service.TerminalMsgProcessService;
import com.eminem.vo.PackageData;
import com.eminem.vo.Session;
import com.eminem.vo.TerminalAuthenticationMsg;
import com.eminem.vo.TerminalRegisterMsg;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.ReferenceCountUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TCPServerHandler extends ChannelInboundHandlerAdapter { // (1)

	private final Logger logger = LoggerFactory.getLogger(getClass());

	private final SessionManager sessionManager;
	private final MsgDecoder decoder;
	private TerminalMsgProcessService msgProcessService;

	public TCPServerHandler() {
		this.sessionManager = SessionManager.getInstance();
		this.decoder = new MsgDecoder();
		this.msgProcessService = new TerminalMsgProcessService();
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws InterruptedException { // (2)
		try {
			ByteBuf buf = (ByteBuf) msg;
			logger.debug("接收到的消息长度：{}",buf.readableBytes());
			if (buf.readableBytes() <= 0) {
				// ReferenceCountUtil.safeRelease(msg);
				return;
			}

			byte[] bs = new byte[buf.readableBytes()];
			buf.readBytes(bs);
			logger.debug("读完后的数据长度：{}" , buf.readableBytes());
			// 字节数据转换为针对于808消息结构的实体类
			PackageData pkg = this.decoder.bytes2PackageData(bs);
			// 引用channel,以便回送数据给硬件
			pkg.setChannel(ctx.channel());
			this.processPackageData(pkg);
		} finally {
			release(msg);
		}
	}

	/**
	 * 
	 * 处理业务逻辑
	 * 
	 * @param packageData
	 * 
	 */
	private void processPackageData(PackageData packageData) {
		final PackageData.MsgHeader header = packageData.getMsgHeader();
						
		/**
		 * 1、终端注册 （0100）2、注册应答（8100）3、注册成功 4、鉴权信息（0102）5、鉴权信息(0102) 6、鉴权应答（8001）7、定位数据（0200）8、平台通用应答（8001）
		 */
		if(0x0100 == header.getMsgId()){
			System.out.println("终端注册");
		}else if(0x8100 == header.getMsgId()){
			System.out.println("注册应答");
		}else if(0x0102 == header.getMsgId()){
			System.out.println("鉴权信息");
		}else if(0x8001 == header.getMsgId()){
			System.out.println("鉴权应答");
		}else if(0x0200 == header.getMsgId()){
			System.out.println("定位数据");
		}
		
		
		
		// 1. 终端心跳-消息体为空 ==> 平台通用应答
		if (TPMSConsts.msg_id_terminal_heart_beat == header.getMsgId()) {
			logger.info(">>>>>[终端心跳],phone={},flowid={}", header.getTerminalPhone(), header.getFlowId());
			try {
				this.msgProcessService.processTerminalHeartBeatMsg(packageData);
				logger.info("<<<<<[终端心跳],phone={},flowid={}", header.getTerminalPhone(), header.getFlowId());
			} catch (Exception e) {
				logger.error("<<<<<[终端心跳]处理错误,phone={},flowid={},err={}", header.getTerminalPhone(), header.getFlowId(),
						e.getMessage());
				e.printStackTrace();
			}
		}

		// 5. 终端鉴权 ==> 平台通用应答
		else if (TPMSConsts.msg_id_terminal_authentication == header.getMsgId()) {
			logger.info(">>>>>[终端鉴权],phone={},flowid={}", header.getTerminalPhone(), header.getFlowId());
			try {
				TerminalAuthenticationMsg authenticationMsg = new TerminalAuthenticationMsg(packageData);
				this.msgProcessService.processAuthMsg(authenticationMsg);
				logger.info("<<<<<[终端鉴权],phone={},flowid={}", header.getTerminalPhone(), header.getFlowId());
			} catch (Exception e) {
				logger.error("<<<<<[终端鉴权]处理错误,phone={},flowid={},err={}", header.getTerminalPhone(), header.getFlowId(),
						e.getMessage());
				e.printStackTrace();
			}
		}
		// 6. 终端注册 ==> 终端注册应答
		else if (TPMSConsts.msg_id_terminal_register == header.getMsgId()) {
			logger.info(">>>>>[终端注册],phone={},flowid={}", header.getTerminalPhone(), header.getFlowId());
			try {
				TerminalRegisterMsg msg = this.decoder.toTerminalRegisterMsg(packageData);
				this.msgProcessService.processRegisterMsg(msg);
				logger.info("<<<<<[终端注册],phone={},flowid={}", header.getTerminalPhone(), header.getFlowId());
			} catch (Exception e) {
				logger.error("<<<<<[终端注册]处理错误,phone={},flowid={},err={}", header.getTerminalPhone(), header.getFlowId(),
						e.getMessage());
				e.printStackTrace();
			}
		}
		// 7. 终端注销(终端注销数据消息体为空) ==> 平台通用应答
		else if (TPMSConsts.msg_id_terminal_log_out == header.getMsgId()) {
			logger.info(">>>>>[终端注销],phone={},flowid={}", header.getTerminalPhone(), header.getFlowId());
			try {
				this.msgProcessService.processTerminalLogoutMsg(packageData);
				logger.info("<<<<<[终端注销],phone={},flowid={}", header.getTerminalPhone(), header.getFlowId());
			} catch (Exception e) {
				logger.error("<<<<<[终端注销]处理错误,phone={},flowid={},err={}", header.getTerminalPhone(), header.getFlowId(),
						e.getMessage());
				e.printStackTrace();
			}
		}
		// 其他情况
		else {
			logger.error(">>>>>>[未知消息类型],phone={},msgId={},package={}", header.getTerminalPhone(), header.getMsgId(),
					packageData);
		}
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) { // (4)
		logger.error("发生异常:{}", cause.getMessage());
		cause.printStackTrace();
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		Session session = Session.buildSession(ctx.channel());
		sessionManager.put(session.getId(), session);
		logger.debug("终端连接:{}", session);
		logger.debug("当前客户端数量：{}",sessionManager.toList().size());
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		final String sessionId = ctx.channel().id().asLongText();
		Session session = sessionManager.findBySessionId(sessionId);
		this.sessionManager.removeBySessionId(sessionId);
		logger.debug("终端断开连接:{}", session);
		logger.debug("当前客户端数量：{}",sessionManager.toList().size());
		ctx.channel().close();
		// ctx.close();
	}

	@Override
	public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
		if (IdleStateEvent.class.isAssignableFrom(evt.getClass())) {
			IdleStateEvent event = (IdleStateEvent) evt;
			if (event.state() == IdleState.READER_IDLE) {
				Session session = this.sessionManager.removeBySessionId(Session.buildId(ctx.channel()));
				logger.error("服务器主动断开连接:{}", session);
				ctx.close();
			}
		}
	}

	private void release(Object msg) {
		try {
			ReferenceCountUtil.release(msg);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}