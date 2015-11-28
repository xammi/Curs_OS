cd driver/mouse
sh setmod.sh

echo '\n>>> Start of proxy <<<\n'
cd ../../proxy
python2 main.py server
