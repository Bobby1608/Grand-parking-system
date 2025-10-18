#include "ParkingSystem.h"
#include <sstream>
#include <iomanip>
#include <cmath>

AdvancedParkingSystem::AdvancedParkingSystem(int totalSlots)
{
    this->totalSlots = totalSlots;
    initializeSlots(totalSlots);
}

void AdvancedParkingSystem::initializeSlots(int totalSlots)
{
    for (int i = 1; i <= totalSlots; i++)
    {
        parkingSlots.push_back(ParkingSlot(i));
        availableSlotsHeap.push(i);
    }
}

int AdvancedParkingSystem::parkVehicle(const string &plateNumber, const string &vehicleType)
{
    if (availableSlotsHeap.empty())
    {
        throw runtime_error("Parking lot is full.");
    }
    if (plateToSlotMap.count(plateNumber))
    {
        throw runtime_error("Vehicle with this plate is already parked.");
    }

    int slotId = availableSlotsHeap.top();
    availableSlotsHeap.pop();

    parkingSlots[slotId - 1].occupy(plateNumber, vehicleType);
    plateToSlotMap[plateNumber] = slotId;

    return slotId;
}

double AdvancedParkingSystem::removeVehicle(const string &plateNumber)
{
    if (plateToSlotMap.find(plateNumber) == plateToSlotMap.end())
    {
        throw runtime_error("Vehicle with this plate not found.");
    }

    int slotId = plateToSlotMap[plateNumber];
    time_t parkTime = parkingSlots[slotId - 1].parkTime;

    double fee = calculateFee(parkTime);

    parkingSlots[slotId - 1].vacate();
    plateToSlotMap.erase(plateNumber);
    availableSlotsHeap.push(slotId);

    return fee;
}

double AdvancedParkingSystem::calculateFee(time_t parkTime)
{
    double baseRate = 2.50; // $2.50 per hour
    time_t currentTime = time(nullptr);
    double secondsParked = difftime(currentTime, parkTime);
    double hoursParked = secondsParked / 3600.0;

    // Simple fee: round up to the nearest hour
    return ceil(hoursParked) * baseRate;
}

string AdvancedParkingSystem::getParkingStatus()
{
    stringstream ss;
    for (const auto &slot : parkingSlots)
    {
        // Format: slotId,isOccupied,plateNumber,vehicleType;
        ss << slot.slotId << "," << (slot.isOccupied ? "1" : "0") << "," << (slot.plateNumber.empty() ? "N/A" : slot.plateNumber) << "," << (slot.vehicleType.empty() ? "N/A" : slot.vehicleType) << ";";
    }
    return ss.str();
}