import { BrowserRouter, Route, Routes } from "react-router"
import Registration from "../pages/Registration"
import Splash from "../pages/steps/Splash"
import Step1 from "../pages/steps/Step1"
import Step2 from "../pages/steps/Step2"
import Step3 from "../pages/steps/Step3"
import Step4 from "../pages/steps/Step4"
import Step5 from "../pages/steps/Step5"
import Done from "../pages/steps/Done"
import Status from "../pages/Status"
import StudentDashboard from "../pages/StudentDashboard"
import InstructorDashboard from "../pages/InstructorDashboard"
import AdminDashboard from "../pages/AdminDashboard"
import ProtectedRoutes from "../components/ProtectedRoutes"
import toast, { Toaster } from "react-hot-toast"
import axios from "../api/axios"
import LandingPage from "../pages/LandingPage/LandingPage"
import Email from "../pages/ForgetPassword/Email"
import OTP from "../pages/ForgetPassword/OTP"
import ResetPassword from "../pages/ForgetPassword/ResetPassword"
import { useStateContext } from "../contexts/ContextProvider"
import { useEffect, useState } from "react"
import { useNavigate } from "react-router-dom"
/**
 * e-commerce app for better understanding of filtering and query parameters
 * 
 * space-x-2 is a Tailwind CSS utility class that adds horizontal spacing (margin) between direct children of a flex container.
 * 
 * 
 * 
 * step 5 //bracket notation for loops , if we use user.key itll look for a value of key which doesnt exist (user.key is used for accessing known properties , bracket notation when the property is stored in a variable (in a loop))
 * 
 * --/ always use formData for api calls
 * 
 * 
 * conditional styling notes :
 * //style={{ width:activeMenu?"":""}}
// ${activeMenu?"ml-[300px]":""} `


revise courses component for better url parameters usage

flex-wrap 
array.filter 
map
array.includes //search
parseInt
slice

courses page

registration in sidebar for table advice


// conditional rendering based on route in studentDashboard

flex-wrap essential for overflow , revise profile , courses div specifically

note on search params 



 */

function AppContent() {
  const { setAuth, auth } = useStateContext();
  const navigate = useNavigate();
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    // Only check auth if it hasn't been checked yet
    if (auth === null) {
      const checkAuth = async () => {
        try {
          const res = await axios.get("/auth/me", { withCredentials: true });
          setAuth(res.data);
          console.log("auth data :", res.data);
        } catch (error) {
          setAuth({});
          console.log("An error occurred in checkAuth ",error);
          
          // toast.error("Session expired. Please log in again.");
          if (window.location.pathname !== "/registration") {
            navigate("/", { replace: true });
          }
        } finally {
          setLoading(false);
        }
      };
      checkAuth();
    } else {
      setLoading(false);
    }
  }, []);

  if (loading) return null; // or a spinner

  return (
    <Routes>
      {/* public routes */}
      <Route path="/" element={<LandingPage />} />
      <Route path="/registration" element={<Registration />} />
      <Route path="/forget-password" element={<Email />} />
      <Route path="/OTP" element={<OTP />} />
      <Route path="/reset-password" element={<ResetPassword />} />
      <Route path="/registration/splash" element={<Splash />} />
      <Route path="/registration/step1" element={<Step1 />} />
      <Route path="/registration/step2" element={<Step2 />} />
      <Route path="/registration/step3" element={<Step3 />} />
      <Route path="/registration/step4" element={<Step4 />} />
      <Route path="/registration/step5" element={<Step5 />} />
      <Route path="/registration/done" element={<Done />} />
      <Route path="/status" element={<Status />} />
      {/* protected routes */}
      <Route element={<ProtectedRoutes allowedRoles={['student']} />}> 
        <Route path="/studentDashboard/*" element={<StudentDashboard />} />
      </Route>
      <Route element={<ProtectedRoutes allowedRoles={['instructor']} />}> 
        <Route path="/instructorDashboard/*" element={<InstructorDashboard />} />
      </Route>
      <Route element={<ProtectedRoutes allowedRoles={['admin']} />}> 
        <Route path="/adminDashboard/*" element={<AdminDashboard />} />
      </Route>
    </Routes>
  );
}

function App() {
  return (
    <div>
      <BrowserRouter>
        <AppContent />
      </BrowserRouter>
      <Toaster />
    </div>
  );
}

export default App
