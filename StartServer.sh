#!/bin/bash
echo "Starting CyberHeist Server..."
java -cp target/classes com.cyberheist.server.CyberHeistServer
read -p "Press enter to continue..." 