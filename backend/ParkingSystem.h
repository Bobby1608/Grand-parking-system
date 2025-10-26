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
#include <iomanip>
#include <sstream>
#include <cmath>
#include <algorithm> // Needed for std::max

using namespace std;

// --- User Profile System ---
enum UserType
{
    GUEST,
    RESIDENT,
    HOTEL,
    STAFF
};

struct UserProfile
{
    string plateNumber;
    string driverName;
    UserType type;
    string billingId; // e.g., Apt 5B, Room 301

    UserProfile() : type(GUEST), billingId("N/A") {}
    UserProfile(string plate, string name, UserType t, string id)
        : plateNumber(plate), driverName(name), type(t), billingId(id) {}
};

struct Vehicle
{
    string plateNumber;
    string vehicleType;
    string driverName;
};

// --- Parking Slot Structure ---
struct ParkingSlot
{
    int slotId;
    string plateNumber;
    string driverName;
    string vehicleType;
    time_t entryTime;

    int floor;
    string vehicleTypeConstraint; // "BIKE", "CAR_GUEST", "CAR_HOTEL", "CAR_RESIDENT", "TRUCK", "STAFF"

    bool isOccupied;
    bool isReserved;

    // --- New Billing Fields ---
    bool isValet;
    time_t prePaidExitTime;
    bool isValidated; // For restaurant discount

    ParkingSlot(int id, int f, string constraint) : slotId(id), floor(f), vehicleTypeConstraint(constraint),
                                                    isOccupied(false), isReserved(false), isValet(false),
                                                    prePaidExitTime(0), isValidated(false), entryTime(0) {}

    void occupy(const string &plate, const string &name, const string &type, bool valet, time_t exitTime = 0)
    {
        plateNumber = plate;
        driverName = name;
        vehicleType = type;
        entryTime = time(nullptr);
        isValet = valet;
        prePaidExitTime = exitTime; // 0 for non-guests
        isOccupied = true;
        isReserved = false;
    }

    void vacate()
    {
        plateNumber = "";
        driverName = "";
        vehicleType = "";
        isOccupied = false;
        isReserved = false;
        isValet = false;
        prePaidExitTime = 0;
        isValidated = false;
        entryTime = 0;
    }
};

class AdvancedParkingSystem
{
private:
    vector<ParkingSlot> parkingSlots;
    map<string, int> plateToSlotMap; // Links plate -> slotId

    // --- Databases ---
    map<string, UserProfile> userDatabase; // Links plate -> UserProfile

    // --- Slot/Queue Management ---
    map<string, priority_queue<int, vector<int>, greater<int>>> typeToAvailableSlots;
    map<string, queue<Vehicle>> typeToWaitingQueue;

    // --- Analytics ---
    vector<pair<time_t, double>> revenueLogs;
    vector<time_t> entryLogs;
    map<UserType, int> occupancyByUserType;

    // --- Billing Constants ---
    const double BIKE_RATE = 20.0;
    const double CAR_RATE = 50.0;
    const double TRUCK_RATE = 100.0;
    const double OVERSTAY_PENALTY_RATE = 2.0; // 2x normal rate for overstay
    const double VALET_FEE = 150.0;
    const int VALIDATION_HOURS = 2; // 2 hours free for restaurant

    void initializeSlots();
    string calculateFee(ParkingSlot &slot);
    Vehicle findNextWaitingVehicle(const string &constraint);

public:
    AdvancedParkingSystem(); // Constructor
    string registerUser(const string &plate, const string &name, int type, const string &billingId);
    string parkVehicle(const string &plate, const string &type, const string &name, int duration, bool valet);
    string removeVehicle(const string &plate);
    string applyValidation(const string &plate);
    string reserveSlot(const string &plate, const string &type, const string &name); // Placeholder
    string findMyCar(const string &plate);
    string getParkingStatus();
    string getAnalyticsData();
    string getUserList();
    string getSlotDetails(int slotId);
};

#endif // PARKING_SYSTEM_H