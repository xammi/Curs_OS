cd driver/mouse
sudo sh setmod.sh

echo '\n>>> Start of proxy <<<\n'
cd ../../proxy
sudo python2 main.py server &
