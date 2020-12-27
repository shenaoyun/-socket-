package multichat;


import java.net.*;
import javax.swing.*;
import java.awt.event.*;
import java.io.*;

public class Client {
	// 主函数，新建登录窗口
	public static void main(String[] args) {
		new Login();
	}
}

/**
 *  负责客户端的读和写，以及登录和发送的监听
 *  之所以把登录和发送的监听放在这里，是因为要共享一些数据，比如mySocket,textArea
 */
class ClientReadAndPrint extends Thread{
	static Socket mySocket = null;  // 一定要加上static，否则新建线程时会清空
	static JTextField textInput;
	static JTextArea textShow;
	static JFrame chatViewJFrame;
	static BufferedReader in = null;  
	
	static PrintWriter out = null;
	static String userName;
	
	//接收从服务端发送来的消息
	public void run() {
		try {
			in = new BufferedReader(new InputStreamReader(mySocket.getInputStream()));  
			while (true) {
				String str = in.readLine();  // 获取服务端发送的信息
				textShow.append(str + '\n');  // 添加进聊天客户端的文本区域
				textShow.setCaretPosition(textShow.getDocument().getLength());  // 设置滚动条在最下面
			}
		} catch (Exception e) {}
	}
	
	/**********************登录监听(内部类)**********************/
	class LoginListen implements ActionListener{
		JTextField textField;
		JPasswordField pwdField;
		JFrame loginJFrame;  // 登录窗口本身
		
		ChatView chatView = null;
		
		public void setJTextField(JTextField textField) {
			this.textField = textField;
		}
		public void setJPasswordField(JPasswordField pwdField) {
			this.pwdField = pwdField;
		}
		public void setJFrame(JFrame jFrame) {
			this.loginJFrame = jFrame;
		}
		public void actionPerformed(ActionEvent event) {
			userName = textField.getText();
			String userPwd = String.valueOf(pwdField.getPassword());  // getPassword方法获得char数组
			if(userName.length() >= 1 && userPwd.equals("123456")) {  // 密码为123456并且用户名长度大于等于1
				chatView = new ChatView(userName);  // 新建聊天窗口,设置聊天窗口的用户名（静态）
				// 建立和服务器的联系
				try {
					InetAddress addr = InetAddress.getByName(null);  // 获取主机地址
					mySocket = new Socket(addr,8081);  // 客户端套接字
					loginJFrame.setVisible(false); 
					out = new PrintWriter(mySocket.getOutputStream());  
					out.println("用户【" + userName + "】进入聊天室！");  // 发送用户名给服务器
					out.flush();  
				} catch (IOException e) {
					e.printStackTrace();
				}
				// 新建普通读写线程并启动
				ClientReadAndPrint readAndPrint = new ClientReadAndPrint();
				readAndPrint.start();
				ClientFileThread fileThread = new ClientFileThread(userName, chatViewJFrame, out);
				fileThread.start();
			}
			else {
				JOptionPane.showMessageDialog(loginJFrame, "账号或密码错误，请重新输入！", "提示", JOptionPane.WARNING_MESSAGE);
			}
		}
	}
	
	/**********************聊天界面监听(内部类)**********************/
	class ChatViewListen implements ActionListener{
		public void setJTextField(JTextField text) {
			textInput = text;  // 放在外部类，因为其它地方也要用到
		}
		public void setJTextArea(JTextArea textArea) {
			textShow = textArea;  // 放在外部类，因为其它地方也要用到
		}
		public void setChatViewJf(JFrame jFrame) {
			chatViewJFrame = jFrame;  // 放在外部类，因为其它地方也要用到

			chatViewJFrame.addWindowListener(new WindowAdapter() {	// 设置关闭聊天界面的监听
				public void windowClosing(WindowEvent e) {
					out.println("用户【" + userName + "】离开聊天室！");
					out.flush();
					System.exit(0);
				}
			});
		}
		// 监听执行函数
		public void actionPerformed(ActionEvent event) {
			try {
				String str = textInput.getText();
				
				if("".equals(str)) {// 文本框内容为空
					textInput.grabFocus();  
					JOptionPane.showMessageDialog(chatViewJFrame, "输入为空，请重新输入！", "提示", JOptionPane.WARNING_MESSAGE);
					return;
				}
				out.println(userName + " 说:" + str);  // 输出给服务端
				out.flush();  
				textInput.setText("");  // 清空文本框
				textInput.grabFocus();  // 设置焦点
			} catch (Exception e) {}
		}
	}
}



