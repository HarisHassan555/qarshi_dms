#!/bin/bash
sudo cp /etc/mysql/my.cnf /etc/mysql/my.cnf.backup
echo -e "\n[mysqld]\nsort_buffer_size=256M\nread_rnd_buffer_size=256M\n" | sudo tee -a /etc/mysql/my.cnf
sudo service mysql restart
