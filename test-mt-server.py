import socket
from concurrent.futures import ThreadPoolExecutor

# Server details
SERVER_ADDRESS = 'localhost'  # server's address
SERVER_PORT = 1081  # server's port
CONNECTIONS = 10  # Number of concurrent connections

def connect_to_server(index):
    try:
        with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
            s.connect((SERVER_ADDRESS, SERVER_PORT))
            request = f"GET /video.mp4 HTTP/1.1\r\nHost: {SERVER_ADDRESS}\r\n\r\n"
            s.send(request.encode())
            # Initialize a variable to store the received data
            received_data = b''
            while True:
                data = s.recv(4096)  # Adjust buffer size as needed
                if not data:
                    break  # Exit the loop if no more data is received
                received_data += data
            print(f"Connection {index}: Request sent, received initial response")
    except Exception as e:
        print(f"Connection {index}: Failed with {e}")

def main():
    with ThreadPoolExecutor(max_workers=CONNECTIONS) as executor:
        executor.map(connect_to_server, range(CONNECTIONS))

if __name__ == "__main__":
    main()
