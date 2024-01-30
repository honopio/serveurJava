package serveur;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.Scanner;

public class AfficheRequetesHttp {

	public static void main(String[] args) {

		if (args.length != 2) {
			System.err.println("Usage: java " + AfficheRequetesHttp.class.getName() + "portnumber directory");
			//il faut pas pouvoir remonter plus loin que le dir du serveur. pb de sécu
			System.exit(1);
		}

		int portNumber = 0;
		try {
			portNumber = Integer.parseInt(args[0]);
			if (portNumber < 0) {
				System.err.println(args[0] + " is not a positive integer");
			}
		} catch (NumberFormatException e) {
			System.err.println(args[0] + " is not an integer");
			System.err.println("Usage: java " + AfficheRequetesHttp.class.getName() + "portnumber directory");
			System.exit(2);
		}

		try (ServerSocket s = new ServerSocket(portNumber);) {

			while (true) {

				try (Socket c = s.accept();) {
					Scanner sc = new Scanner(c.getInputStream());
					System.out.println("----DEBUT REQUETE----");
					while (sc.hasNextLine()) {
						String line = sc.nextLine();
						// on sort de la boucle si on arrive sur la ligne vide de fin des headers
						if ("".equals(line)) {
							break;
						}
						System.out.println("-" + line + "-");
					}
					System.out.println("----FIN REQUETE----");

					OutputStream os = c.getOutputStream();

					// exemple d'envoi d'un contenu html
					/*Writer w = new OutputStreamWriter(os);
					PrintWriter pw = new PrintWriter(w);
					pw.println("HTTP/1.1 200 OK");
					pw.println("Content-Type: text/html");
					pw.println();// ligne vide pour signaler la fin des entÃªtes 
					//CORPS DE LA REQUETE : (sera le code source de la ressources envoyée)
					pw.println("<!DOCTYPE html PUBLIC \"-//IETF//DTD HTML 2.0//EN\">");
					pw.println("<html><body>");
					pw.println("<h1>Hello DCISS !</h1>");
					pw.println("</body></html>");
					pw.flush(); // il faut vider le buffer pour que le contenu soit envoyÃ©.
					*/
					
					// exemple d'envoi de fichier
					Path fileToSend = Paths.get("image.png");
					String mimeType = Files.probeContentType(fileToSend);

					Writer w = new OutputStreamWriter(os);
					PrintWriter pw = new PrintWriter(w);
					pw.println("HTTP/1.1 200 OK");
					pw.println("Content-Type: " + mimeType);
					pw.println();// ligne vide pour signaler la fin des entÃªtes
					pw.flush();// il faut vider le buffer pour que le contenu soit envoyÃ©. envoie à OutputStream os je crois

					Files.copy(fileToSend, os); // ici pas besoin de vider le buffer os n'est pas buffeurisÃ©

				}

			}
		} catch (IOException e) {
			System.err.println("IO error: ");
			e.printStackTrace();
			System.exit(10);
		}

	}
}