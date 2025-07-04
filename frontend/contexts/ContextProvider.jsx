
import React, { useContext, useState } from "react"
import { useLocation } from "react-router"
const stateContext=React.createContext()

export const ContextProvider = ({children})=>{
  const [user, setUser] = useState({
    email : "",
    password : "",
    firstName: "",
    lastName: "",
    dateOfBirth: "", // Ensure it's initially empty
    gender: "",
    phoneNumber: "+20",//check later
    address: "",
    city:"",
    country:"",
    highSchoolName: "",
    highSchoolGpa:"",
    highSchoolCertificate: null,
    facultyApplied: "FCI",
    preferredMajor: "",
    IDNumber: "",
    IdPhoto:null,
    personalPhoto :null,
    status: "accepted", 
    //below is optional for testing profile management
    
   

  })

  const [updatedProfileData, setupdatedProfileData] = useState({
    firstName:"",
    lastName:"",
    city:"",
    country:"",
    address:"",
    personalImage:null
    //no personal image here , added manually below
  });
  const [activeMenu, setActiveMenu] = useState(false)
  const [profile, setProfile] = useState(false)

  const [meter, setMeter] = useState(1)
  const incrementMeter= ()=>{
    setMeter((prev)=>prev+1)
  }
  const decrementMeter= ()=>{
    setMeter((prev)=>prev-1)
  }
 
  const [auth, setAuth] = useState(null)

  const Placeholder = "EDU Mate"
   return (
    <stateContext.Provider value={{
        Placeholder,user,setUser,
        updatedProfileData,setupdatedProfileData,
        meter,incrementMeter,setMeter,decrementMeter,
        activeMenu,setActiveMenu,
        profile,setProfile,
        auth ,setAuth
    }}>
        {children}
    </stateContext.Provider>
   )

}

export const useStateContext = ()=>useContext(stateContext)

// 4 steps : 1- create a context 2- create a function to act as a provider , having children , in this function define any states or functions that will be used globally , and return children 3- make a custom hook that uses the useCOntext(stateContext) 4- wrap app in provider  5- destructure the useStateContext to get what you need in any component