#include "ParkingSystem.h"
#include <vector>
#include <string>

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
    AdvancedParkingSystem parkingSystem;

    string commandLine;
    while (getline(cin, commandLine))
    {
        vector<string> args = split(commandLine, ',');
        string command = args[0];

        try
        {
            if (command == "PARK" && args.size() == 6)
            {
                // PARK,plate,type,name,duration,valet(1/0)
                cout << parkingSystem.parkVehicle(args[1], args[2], args[3], stoi(args[4]), stoi(args[5])) << endl;
            }
            else if (command == "REMOVE" && args.size() == 2)
            {
                // REMOVE,plate
                cout << parkingSystem.removeVehicle(args[1]) << endl;
            }
            else if (command == "VALIDATE" && args.size() == 2)
            {
                // VALIDATE,plate
                cout << parkingSystem.applyValidation(args[1]) << endl;
            }
            else if (command == "FIND" && args.size() == 2)
            {
                // FIND,plate
                cout << parkingSystem.findMyCar(args[1]) << endl;
            }
            else if (command == "REGISTER" && args.size() == 5)
            {
                // REGISTER,plate,name,type(0-3),billingId
                cout << parkingSystem.registerUser(args[1], args[2], stoi(args[3]), args[4]) << endl;
            }
            else if (command == "STATUS")
            {
                cout << "STATUS," << parkingSystem.getParkingStatus() << endl;
            }
            else if (command == "GET_ANALYTICS")
            {
                cout << "ANALYTICS," << parkingSystem.getAnalyticsData() << endl;
            }
            else if (command == "GET_USERS")
            {
                cout << "USERS," << parkingSystem.getUserList() << endl;
            }
            else if (command == "GET_DETAILS" && args.size() == 2)
            {
                // GET_DETAILS,slotId
                cout << parkingSystem.getSlotDetails(stoi(args[1])) << endl;
            }
            else
            {
                cout << "ERROR,Invalid command syntax: " << commandLine << endl;
            }
        }
        catch (const exception &e)
        {
            // Catch standard exceptions (like stoi errors)
            cout << "ERROR," << e.what() << endl;
        }
        catch (...)
        {
            // Catch any other unexpected errors
            cout << "ERROR,An unexpected backend error occurred." << endl;
        }
        cout.flush(); // Ensure output is sent immediately
    }
    return 0;
}