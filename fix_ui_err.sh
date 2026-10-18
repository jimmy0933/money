#!/bin/bash
sed -i 's/var errorMessage by remember { mutableStateOf<String?>/val errorMessage = remember { mutableStateOf<String?>/' app/src/main/java/com/moneytracker/app/ui/screens/LoansScreen.kt
