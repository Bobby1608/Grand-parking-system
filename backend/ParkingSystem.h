#ifndef PARKING_SYSTEM_H
#define PARKING_SYSTEM_H

#include <iostream>
#include <vector>
#include <queue>
#include <map>
#include <set>
#include <string>
#include <ctime>
#include <stdexcept>

using namespace std;

class AdvancedParkingSystem
{
private:
    struct ParkingSlot
    {
        int slotId;
        string plateNumber;
        time_t parkTime;
        string vehicleType;
        bool isOccupied;

        ParkingSlot(int id) : slotId(id), isOccupied(false), plateNumber(""), vehicleType("") {}

        void occupy(const string &plate, const string &type)
        {
            this->plateNumber = plate;
            this->vehicleType = type;
            this->parkTime = time(nullptr);
            this->isOccupied = true;
        }

        void vacate()
        {
            this->plateNumber = "";
            this->vehicleType = "";
            this->isOccupied = false;
        }
    };

    vector<ParkingSlot> parkingSlots;
    priority_queue<int, vector<int>, greater<int>> availableSlotsHeap; // Min-heap for nearest available slot
    map<string, int> plateToSlotMap;                                   // Hash table for O(1) lookup
    int totalSlots;

public:
    AdvancedParkingSystem(int totalSlots);

    // Core Functions
    int parkVehicle(const string &plateNumber, const string &vehicleType);
    double removeVehicle(const string &plateNumber);
    string getParkingStatus(); // Returns a string summary of the lot status

private:
    void initializeSlots(int totalSlots);
    double calculateFee(time_t parkTime);
};

#endif // PARKING_SYSTEM_H