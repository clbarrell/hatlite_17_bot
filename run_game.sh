#!/bin/sh

javac MyBot.java
javac OldBot3.java
./halite -d "240 160" "java MyBot" "java OldBot3"
