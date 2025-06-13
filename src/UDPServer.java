import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class UDPServer {
    private static final int PORT = 12345;
    // Mapeia SocketAddress (IP+porta) → nome de usuário
    private final Map<SocketAddress, String> clients = new ConcurrentHashMap<>();

    public void start() throws IOException {
        DatagramSocket socket = new DatagramSocket(PORT);
        System.out.println("Servidor UDP escutando na porta " + PORT + "...");
        byte[] buffer = new byte[1024];

        while (true) {
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            socket.receive(packet);
            // delega a lógica de processamento a uma thread
            new Thread(() -> {
                try {
                    handlePacket(packet, socket);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }

    private void handlePacket(DatagramPacket packet, DatagramSocket socket) throws IOException {
        String msg = new String(packet.getData(), 0, packet.getLength(), "UTF-8").trim();
        SocketAddress addr = packet.getSocketAddress();

        // Registro de usuário: "REGISTER:username"
        if (msg.startsWith("REGISTER:")) {
            String username = msg.substring("REGISTER:".length());
            clients.put(addr, username);
            System.out.printf("[+] %s registrado em %s%n", username, addr);
            return;
        }

        // Mensagem privada: "@user:texto"
        // Mensagem privada: "@user:texto"
        if (msg.startsWith("@")) {
            String[] parts = msg.substring(1).split(":", 2);
            if (parts.length == 2) {
                String target = parts[0];
                String text   = parts[1];
                String sender = clients.getOrDefault(addr, addr.toString());
                // procura o SocketAddress do destinatário
                for (Map.Entry<SocketAddress,String> entry : clients.entrySet()) {
                    if (entry.getValue().equals(target)) {
                        InetSocketAddress dest = (InetSocketAddress) entry.getKey();
                        String out = String.format("[PRIV] %s: %s", sender, text);
                        byte[] outBytes = out.getBytes("UTF-8");
                        DatagramPacket dp = new DatagramPacket(
                                outBytes,
                                outBytes.length,
                                dest.getAddress(),
                                dest.getPort()
                        );
                        socket.send(dp);
                        break;  // só para um destinatário
                    }
                }
            }
            return;
        }


        // Broadcast para todos (exceto quem enviou)
        String sender = clients.getOrDefault(addr, addr.toString());
        String out = String.format("[ALL] %s: %s", sender, msg);
        byte[] outBytes = out.getBytes("UTF-8");
        for (SocketAddress clientAddr : clients.keySet()) {
            if (!clientAddr.equals(addr)) {
                // clientAddr é, na verdade, um InetSocketAddress:
                InetSocketAddress target = (InetSocketAddress) clientAddr;
                DatagramPacket dp = new DatagramPacket(
                        outBytes,
                        outBytes.length,
                        target.getAddress(),
                        target.getPort()
                );
                socket.send(dp);
            }
        }
    }

    public static void main(String[] args) {
        try {
            new UDPServer().start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
