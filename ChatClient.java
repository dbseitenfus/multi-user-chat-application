import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;
import java.awt.BorderLayout;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.*;
import java.awt.Color;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * A simple Swing-based client for the chat server. Graphically it is a frame
 * with a text field for entering messages and a textarea to see the whole
 * dialog.
 *
 * The client follows the following Chat Protocol. When the server sends
 * "SUBMITNAME" the client replies with the desired screen name. The server will
 * keep sending "SUBMITNAME" requests as long as the client submits screen names
 * that are already in use. When the server sends a line beginning with
 * "NAMEACCEPTED" the client is now allowed to start sending the server
 * arbitrary strings to be broadcast to all chatters connected to the server.
 * When the server sends a line beginning with "MESSAGE" then all characters
 * following this string should be displayed in its message area.
 */
public class ChatClient {

    String serverAddress;
    Scanner in;
    PrintWriter out;
    JFrame frame = new JFrame("Chatter");
    JTextField textField = new JTextField(50);
    JTextPane messageArea = new JTextPane(); 
    StyledDocument doc = messageArea.getStyledDocument();

    /**
     * Constructs the client by laying out the GUI and registering a listener with
     * the textfield so that pressing Return in the listener sends the textfield
     * contents to the server. Note however that the textfield is initially NOT
     * editable, and only becomes editable AFTER the client receives the
     * NAMEACCEPTED message from the server.
     */
    public ChatClient(String serverAddress) {
        this.serverAddress = serverAddress;

        // Set up styles
        setupStyles();

        textField.setEditable(false);
        messageArea.setEditable(false);
        messageArea.setBackground(Color.LIGHT_GRAY);

        frame.getContentPane().add(textField, BorderLayout.SOUTH);
        frame.getContentPane().add(new JScrollPane(messageArea), BorderLayout.CENTER);
        frame.pack();

        // Send on enter then clear to prepare for next message
        textField.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                out.println(textField.getText());
                textField.setText("");
            }
        });
    }

    private void setupStyles() {
        // Style for system messages (blue and bold)
        Style systemStyle = messageArea.addStyle("System", null);
        StyleConstants.setForeground(systemStyle, Color.BLUE);
        StyleConstants.setBold(systemStyle, true);

        // Style for regular messages (black)
        Style messageStyle = messageArea.addStyle("Message", null);
        StyleConstants.setForeground(messageStyle, Color.BLACK);

        // Style for user messages (white, bold)
        Style userMessageStyle = messageArea.addStyle("UserMessage", null);
        StyleConstants.setForeground(userMessageStyle, Color.WHITE);
        StyleConstants.setBold(userMessageStyle, true);

        // Style for SUBMITNAME messages (green)
        Style submitNameStyle = messageArea.addStyle("SubmitName", null);
        StyleConstants.setForeground(submitNameStyle, Color.GREEN);
        StyleConstants.setBold(submitNameStyle, true);

        // Style for NAMEACCEPTED messages (purple)
        Style nameAcceptedStyle = messageArea.addStyle("NameAccepted", null);
        StyleConstants.setForeground(nameAcceptedStyle, new Color(128, 0, 128)); 
        StyleConstants.setBold(nameAcceptedStyle, true);
    }


    private void appendMessage(String message, String style) {
        try {
            String timeStamp = new SimpleDateFormat("HH:mm:ss").format(new Date());
            doc.insertString(doc.getLength(), "[" + timeStamp + "] " + message + "\n", messageArea.getStyle(style));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getName() {
        return JOptionPane.showInputDialog(frame, "Choose a screen name:", "Screen name selection",
                JOptionPane.PLAIN_MESSAGE);
    }

    private void run() throws IOException {
        try {
            var socket = new Socket(serverAddress, 59001);
            in = new Scanner(socket.getInputStream());
            out = new PrintWriter(socket.getOutputStream(), true);

            while (in.hasNextLine()) {
                var line = in.nextLine();
                if (line.startsWith("SUBMITNAME")) {
                    out.println(getName());
                    appendMessage("Please enter your screen name:", "SubmitName");
                } else if (line.startsWith("NAMEACCEPTED")) {
                    this.frame.setTitle("Chatter - " + line.substring(13));
                    textField.setEditable(true);
                    appendMessage("Welcome! Your name has been accepted.", "NameAccepted");
                } else if (line.startsWith("SYSTEM")) {
                    appendMessage(line.substring(7), "System");
                } else if (line.startsWith("MESSAGE")) {
                    appendMessage(line.substring(8), "UserMessage");
                }
            }
        } finally {
            frame.setVisible(false);
            frame.dispose();
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.err.println("Pass the server IP as the sole command line argument");
            return;
        }
        var client = new ChatClient(args[0]);
        client.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        client.frame.setVisible(true);
        client.run();
    }
}
