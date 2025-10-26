#include "ParkingSystem.h"

// --- Constructor ---
AdvancedParkingSystem::AdvancedParkingSystem()
{
    initializeSlots();
    // Pre-register some users for demo
    registerUser("MH01AA1111", "Bhuban W.", 1, "Apt 5B");         // Resident
    registerUser("MH02BB2222", "Mayur N.", 2, "Room 301");        // Hotel
    registerUser("MH03CC3333", "Aditya S.", 3, "Building Staff"); // Staff
}

// --- NEW 6-FLOOR LAYOUT ---
void AdvancedParkingSystem::initializeSlots()
{
    int currentId = 1;
    // Floor 1: 15 Bike slots
    for (int i = 0; i < 15; i++)
    {
        parkingSlots.push_back(ParkingSlot(currentId, 1, "BIKE"));
        typeToAvailableSlots["BIKE"].push(currentId);
        currentId++;
    }
    // Floor 2: 20 Car slots (Restaurant Guests)
    for (int i = 0; i < 20; i++)
    {
        parkingSlots.push_back(ParkingSlot(currentId, 2, "CAR_GUEST"));
        typeToAvailableSlots["CAR_GUEST"].push(currentId);
        currentId++;
    }
    // Floor 3: 15 Car slots (Hotel Guests)
    for (int i = 0; i < 15; i++)
    {
        parkingSlots.push_back(ParkingSlot(currentId, 3, "CAR_HOTEL"));
        typeToAvailableSlots["CAR_HOTEL"].push(currentId);
        currentId++;
    }
    // Floor 4: 15 Car slots (Residents)
    for (int i = 0; i < 15; i++)
    {
        parkingSlots.push_back(ParkingSlot(currentId, 4, "CAR_RESIDENT"));
        typeToAvailableSlots["CAR_RESIDENT"].push(currentId);
        currentId++;
    }
    // Floor 5: 10 Truck/Delivery slots
    for (int i = 0; i < 10; i++)
    {
        parkingSlots.push_back(ParkingSlot(currentId, 5, "TRUCK"));
        typeToAvailableSlots["TRUCK"].push(currentId);
        currentId++;
    }
    // Floor 6: 10 Staff slots (Cars/Bikes)
    for (int i = 0; i < 10; i++)
    {
        parkingSlots.push_back(ParkingSlot(currentId, 6, "STAFF"));
        typeToAvailableSlots["STAFF"].push(currentId);
        currentId++;
    }
}

// --- User Management ---
string AdvancedParkingSystem::registerUser(const string &plate, const string &name, int type, const string &billingId)
{
    if (plate.empty() || name.empty() || billingId.empty())
    {
        return "ERROR,All fields are required for registration.";
    }
    if (userDatabase.count(plate))
    {
        return "ERROR,Plate already registered.";
    }
    userDatabase[plate] = UserProfile(plate, name, (UserType)type, billingId);
    return "SUCCESS,User " + name + " registered.";
}

// --- Parking Logic ---
string AdvancedParkingSystem::parkVehicle(const string &plate, const string &type, const string &name, int duration, bool valet)
{
    if (plate.empty())
        return "ERROR,Plate number cannot be empty.";
    if (plateToSlotMap.count(plate))
        return "ERROR,Vehicle already parked.";

    UserProfile profile;
    if (userDatabase.count(plate))
    {
        profile = userDatabase[plate]; // Load existing profile
    }
    else
    {
        profile = UserProfile(plate, name.empty() ? "Guest" : name, GUEST, "N/A"); // Create guest profile
    }

    string constraint;
    time_t exitTime = 0;
    double fee = 0;

    // Determine parking zone based on user type and vehicle type
    if (profile.type == RESIDENT)
    {
        constraint = "CAR_RESIDENT";
    }
    else if (profile.type == STAFF)
    {
        constraint = (type == "TRUCK") ? "TRUCK" : "STAFF"; // Staff can park trucks on floor 5
    }
    else if (profile.type == HOTEL)
    {
        constraint = "CAR_HOTEL";
    }
    else
    { // GUEST
        if (type == "BIKE")
            constraint = "BIKE";
        else if (type == "TRUCK")
            constraint = "TRUCK";
        else
            constraint = "CAR_GUEST";

        // Guests must pre-pay
        double baseRate = (type == "BIKE") ? BIKE_RATE : (type == "TRUCK" ? TRUCK_RATE : CAR_RATE);
        fee = max(1.0, (double)duration) * baseRate; // Ensure at least 1 hour charge
        if (valet)
            fee += VALET_FEE;
        exitTime = time(nullptr) + max(1, duration) * 3600; // Store exit time based on pre-paid duration
        revenueLogs.push_back({time(nullptr), fee});
    }

    // Find and occupy a slot
    if (typeToAvailableSlots.count(constraint) && !typeToAvailableSlots[constraint].empty())
    {
        int slotId = typeToAvailableSlots[constraint].top();
        typeToAvailableSlots[constraint].pop();

        parkingSlots[slotId - 1].occupy(plate, profile.driverName, type, valet, exitTime);
        plateToSlotMap[plate] = slotId;
        occupancyByUserType[profile.type]++;
        entryLogs.push_back(time(nullptr));

        if (fee > 0)
        {
            stringstream ss;
            ss << fixed << setprecision(2) << fee;
            return "SUCCESS,Parked in slot " + to_string(slotId) + " (Floor " + to_string(parkingSlots[slotId - 1].floor) + "). Pre-paid: \u20B9" + ss.str();
        }
        return "SUCCESS,Welcome. Parked in slot " + to_string(slotId) + " (Floor " + to_string(parkingSlots[slotId - 1].floor) + ").";
    }

    // No slots, add to waiting queue
    typeToWaitingQueue[constraint].push({plate, type, profile.driverName});
    return "SUCCESS,Lot full for " + constraint + ". Vehicle " + plate + " added to waiting queue.";
}

// --- Exit Logic ---
string AdvancedParkingSystem::removeVehicle(const string &plate)
{
    if (plate.empty())
        return "ERROR,Plate number cannot be empty.";
    if (!plateToSlotMap.count(plate))
        return "ERROR,Vehicle not found.";

    int slotId = plateToSlotMap[plate];
    ParkingSlot &slot = parkingSlots[slotId - 1];
    UserProfile profile = userDatabase.count(plate) ? userDatabase[plate] : UserProfile(plate, slot.driverName, GUEST, "N/A");

    string feeMessage = calculateFee(slot); // Calculate fee BEFORE vacating

    // Vacate the slot
    string constraint = slot.vehicleTypeConstraint;
    slot.vacate();
    plateToSlotMap.erase(plate);
    if (occupancyByUserType.count(profile.type))
    { // Decrement count safely
        occupancyByUserType[profile.type]--;
    }

    // Check waiting queue for this zone
    Vehicle nextVehicle = findNextWaitingVehicle(constraint);
    if (nextVehicle.plateNumber != "")
    {
        // Park the waiting vehicle (assume guest, 1 hour pre-pay, no valet)
        // Note: This recursive call might need careful state management in a real system
        parkVehicle(nextVehicle.plateNumber, nextVehicle.vehicleType, nextVehicle.driverName, 1, false);
    }
    else
    {
        // No one waiting, free the slot
        typeToAvailableSlots[constraint].push(slotId);
    }

    return "SUCCESS," + feeMessage;
}

// --- Billing Logic ---
string AdvancedParkingSystem::calculateFee(ParkingSlot &slot)
{
    UserProfile profile = userDatabase.count(slot.plateNumber) ? userDatabase[slot.plateNumber] : UserProfile();

    stringstream ssFee;
    ssFee << fixed << setprecision(2); // Set precision once

    if (profile.type == RESIDENT || profile.type == STAFF)
    {
        return "User is Resident/Staff. No charge.";
    }

    time_t currentTime = time(nullptr);
    double hoursParked = ceil(max(3600.0, difftime(currentTime, slot.entryTime)) / 3600.0); // Min 1 hour

    double baseRate;
    if (slot.vehicleType == "BIKE")
        baseRate = BIKE_RATE;
    else if (slot.vehicleType == "TRUCK")
        baseRate = TRUCK_RATE;
    else
        baseRate = CAR_RATE; // Default CAR

    if (profile.type == HOTEL)
    {
        double fee = hoursParked * baseRate;
        if (slot.isValet)
            fee += VALET_FEE;
        revenueLogs.push_back({currentTime, fee});
        ssFee << fee;
        return "Bill \u20B9" + ssFee.str() + " to Room " + profile.billingId + ".";
    }

    // --- Guest Logic (Pre-paid & Overstay) ---
    if (currentTime <= slot.prePaidExitTime && !slot.isValidated)
    {
        return "Pre-paid. No additional charge.";
    }

    // Calculate overstay duration
    double overstaySeconds = difftime(currentTime, slot.prePaidExitTime);
    if (slot.isValidated)
    {
        overstaySeconds -= (VALIDATION_HOURS * 3600); // Subtract validated free hours
    }
    if (overstaySeconds <= 0)
    { // If validation covers overstay or they left on time
        return slot.isValidated ? "Validated. No additional charge." : "Pre-paid. No additional charge.";
    }

    // Calculate penalty fee
    double overstayHours = ceil(overstaySeconds / 3600.0);
    double penaltyFee = overstayHours * baseRate * OVERSTAY_PENALTY_RATE;

    // Add original valet fee if applicable and not validated (assuming validation covers valet too)
    if (slot.isValet && !slot.isValidated)
        penaltyFee += VALET_FEE;

    revenueLogs.push_back({currentTime, penaltyFee});
    ssFee << penaltyFee;
    return "OVERSTAY. Penalty fee due: \u20B9" + ssFee.str();
}

// --- Other Features ---
string AdvancedParkingSystem::applyValidation(const string &plate)
{
    if (plate.empty())
        return "ERROR,Plate number cannot be empty.";
    if (!plateToSlotMap.count(plate))
        return "ERROR,Vehicle not found.";
    int slotId = plateToSlotMap[plate];
    parkingSlots[slotId - 1].isValidated = true;
    return "SUCCESS,Validation applied to " + plate + ". " + to_string(VALIDATION_HOURS) + " hours free.";
}

string AdvancedParkingSystem::reserveSlot(const string &plate, const string &type, const string &name)
{
    // Placeholder - Not fully implemented
    return "ERROR,Reservation system not implemented.";
}

string AdvancedParkingSystem::findMyCar(const string &plate)
{
    if (plate.empty())
        return "ERROR,Plate number cannot be empty.";
    if (!plateToSlotMap.count(plate))
        return "ERROR,Vehicle not found in the parking lot.";
    // Send back slotId AND floor
    int slotId = plateToSlotMap[plate];
    int floor = parkingSlots[slotId - 1].floor;
    return "SUCCESS," + to_string(slotId) + "," + to_string(floor);
}

Vehicle AdvancedParkingSystem::findNextWaitingVehicle(const string &constraint)
{
    if (typeToWaitingQueue.count(constraint) && !typeToWaitingQueue[constraint].empty())
    {
        Vehicle next = typeToWaitingQueue[constraint].front();
        typeToWaitingQueue[constraint].pop();
        return next;
    }
    return {"", "", ""}; // Return empty vehicle if no match
}

string AdvancedParkingSystem::getSlotDetails(int slotId)
{
    if (slotId <= 0 || slotId >= parkingSlots.size() + 1)
    { // Check bounds correctly
        return "ERROR,Invalid slot ID.";
    }

    ParkingSlot &slot = parkingSlots[slotId - 1]; // Use 0-based index
    if (!slot.isOccupied)
    {
        return "ERROR,Slot " + to_string(slotId) + " is empty.";
    }

    char timeBuffer[80];
    struct tm *timeinfo = localtime(&slot.entryTime);
    strftime(timeBuffer, sizeof(timeBuffer), "%Y-%m-%d %I:%M:%S %p", timeinfo); // Added seconds

    // Format: Plate,DriverName,EntryTime
    stringstream ss;
    ss << slot.plateNumber << "," << slot.driverName << "," << timeBuffer;
    return "SUCCESS," + ss.str();
}

// --- Status & Analytics (C++11 compatible) ---
string AdvancedParkingSystem::getParkingStatus()
{
    stringstream ss;
    time_t now = time(nullptr);
    for (size_t i = 0; i < parkingSlots.size(); ++i)
    {
        const ParkingSlot &slot = parkingSlots[i];
        bool overstay = (slot.isOccupied && slot.prePaidExitTime > 0 && now > slot.prePaidExitTime && !slot.isValidated);
        ss << slot.slotId << ","
           << (slot.isOccupied ? "1" : "0") << ","
           << (slot.isReserved ? "1" : "0") << ","
           << (overstay ? "1" : "0") << ","
           << (slot.plateNumber.empty() ? "N/A" : slot.plateNumber) << ","
           << slot.floor << ";";
    }
    ss << "|"; // Queue separator

    for (map<string, queue<Vehicle>>::const_iterator it = typeToWaitingQueue.begin(); it != typeToWaitingQueue.end(); ++it)
    {
        if (!it->second.empty())
        {
            ss << it->first << ":" << it->second.size() << ";";
        }
    }
    return ss.str();
}

string AdvancedParkingSystem::getUserList()
{
    stringstream ss;
    for (map<string, UserProfile>::const_iterator it = userDatabase.begin(); it != userDatabase.end(); ++it)
    {
        // Format: Name,Plate,BillingID,Type(as int)
        ss << it->second.driverName << "," << it->second.plateNumber << "," << it->second.billingId << "," << (int)it->second.type << ";";
    }
    return ss.str();
}

string AdvancedParkingSystem::getAnalyticsData()
{
    stringstream ss;
    map<int, int> hourlyDist;
    for (size_t i = 0; i < entryLogs.size(); ++i)
    {
        hourlyDist[localtime(&entryLogs[i])->tm_hour]++;
    }
    for (map<int, int>::const_iterator it = hourlyDist.begin(); it != hourlyDist.end(); ++it)
    {
        ss << it->first << "," << it->second << ";";
    }
    ss << "|";

    for (map<UserType, int>::const_iterator it = occupancyByUserType.begin(); it != occupancyByUserType.end(); ++it)
    {
        if (it->second > 0)
            ss << (int)it->first << "," << it->second << ";";
    }
    ss << "|";

    double totalRevenue = 0;
    for (size_t i = 0; i < revenueLogs.size(); ++i)
    {
        totalRevenue += revenueLogs[i].second;
    }

    stringstream ssFee; // Declare locally
    ssFee << fixed << setprecision(2) << totalRevenue;
    ss << ssFee.str();

    return ss.str();
}