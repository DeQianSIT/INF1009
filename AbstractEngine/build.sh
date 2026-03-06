#!/bin/bash

# Abstract Engine Build Script

echo "======================================"
echo "  Building Abstract Engine"
echo "======================================"
echo ""

# Create bin directory if it doesn't exist
if [ ! -d "bin" ]; then
    mkdir bin
    echo "Created bin directory"
fi

# Compile all Java files
echo "Compiling Java files..."
cd src

# Find and compile all Java files
find . -name "*.java" > sources.txt
javac -d ../bin @sources.txt

# Check compilation status
if [ $? -eq 0 ]; then
    echo ""
    echo "✓ Compilation successful!"
    echo ""
    echo "To run the simulation:"
    echo "  cd bin"
    echo "  java simulation.EngineSimulation"
    echo ""
    
    # Clean up
    rm sources.txt
    
    # Offer to run immediately
    read -p "Run the simulation now? (y/n) " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        cd ../bin
        java simulation.EngineSimulation
    fi
else
    echo ""
    echo "✗ Compilation failed!"
    rm sources.txt
    exit 1
fi
