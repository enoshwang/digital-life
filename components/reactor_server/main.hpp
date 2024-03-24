#ifndef MAIN_HPP
#define MAIN_HPP


void init_log();
bool create_listen_socket(int port, int &sockfd);


inline static const int MAXIMUM_LENGTH_FOR_LISTEN{16};


#endif // MAIN_HPP