import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Scanner;

public class UDPClient {
    private static final String SERVER_IP   = "127.0.0.1";
    private static final int    SERVER_PORT = 12345;

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.out.println("Uso: java UDPClient <seu_nome>");
            return;
        }
        String username = args[0];
        DatagramSocket socket = new DatagramSocket();
        socket.setSoTimeout(500);

        // registro
        String reg = "REGISTER:" + username;
        byte[] regBuf = reg.getBytes("UTF-8");
        socket.send(new DatagramPacket(regBuf, regBuf.length,
                InetAddress.getByName(SERVER_IP), SERVER_PORT));

        // thread para escutar mensagens
        new Thread(() -> {
            byte[] buf = new byte[1024];
            while (true) {
                try {
                    DatagramPacket pkt = new DatagramPacket(buf, buf.length);
                    socket.receive(pkt);
                    String msg = new String(pkt.getData(), 0, pkt.getLength(), "UTF-8");
                    System.out.println(msg);
                } catch (IOException ignored) {}
            }
        }).start();

        // loop de envio
        System.out.println("Digite @user:mensagem para privado ou texto para broadcast:");
        Scanner scanner = new Scanner(System.in);
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            if (line.isEmpty()) continue;
            byte[] data = line.getBytes("UTF-8");
            socket.send(new DatagramPacket(data, data.length,
                    InetAddress.getByName(SERVER_IP), SERVER_PORT));
        }
        scanner.close();
        socket.close();
    }
}
