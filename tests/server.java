import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class server {
    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(5000)) {
            System.out.println("Server listening on port 5000");
            while (true) {
                Socket client = serverSocket.accept();
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(client.getInputStream()));
                PrintWriter out = new PrintWriter(
                        client.getOutputStream(), true);
                String line;
                while ((line = in.readLine()) != null) {
                    out.println("ECHO: " + line);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
