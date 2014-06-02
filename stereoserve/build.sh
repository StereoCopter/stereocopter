gcc cap.c -lpthread -lrt -g | grep --color -E '^|error'
echo "done"
