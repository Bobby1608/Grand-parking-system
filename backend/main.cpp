#include "ParkingSystem.h"
#include <vector>
#include <string>
#include <sstream>
#include <iomanip>

// Helper function to split strings
vector<string> split(const string &s, char delimiter)
{
    vector<string> tokens;
    string token;
    istringstream tokenStream(s);
    while (getline(tokenStream, token, delimiter))
    {
        tokens.push_back(token);
    }
    return tokens;
}

int main()
{
    AdvancedParkingSystem parkingSystem(20); // Initialize with 20 slots

    string commandLine;
    while (getline(cin, commandLine))
    {
        vector<string> args = split(commandLine, ',');
        string command = args[0];

        try
        {
            if (command == "PARK" && args.size() == 3)
            {
                int slotId = parkingSystem.parkVehicle(args[1], args[2]);
                cout << "SUCCESS,Vehicle " << args[1] << " parked in slot " << slotId << "." << endl;
            }
            else if (command == "REMOVE" && args.size() == 2)
            {
                double fee = parkingSystem.removeVehicle(args[1]);
                stringstream ss;
                ss << fixed << setprecision(2) << fee;
                cout << "SUCCESS,Vehicle " << args[1] << " removed. Fee: $" << ss.str() << "." << endl;
            }
            else if (command == "STATUS")
            {
                cout << "STATUS," << parkingSystem.getParkingStatus() << endl;
            }
            else
            {
                cout << "ERROR,Invalid command." << endl;
            }
        }
        catch (const runtime_error &e)
        {
            cout << "ERROR," << e.what() << endl;
        }
    }

    return 0;
}