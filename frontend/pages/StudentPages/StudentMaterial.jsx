import React, { useEffect, useState } from 'react'
import { useNavigate } from 'react-router'
import useAxiosPrivate from '../../hooks/useAxiosPrivate'
import toast from 'react-hot-toast'

const StudentMaterial = () => {
    const [courses, setcourses] = useState([])
    const navigate = useNavigate()
   
    const axiosPrivate = useAxiosPrivate()
    useEffect(()=>{
        const fetchCourses = async ()=>{
            try {
                const response = await axiosPrivate.get("/student/courses/signed")
                setcourses(response.data)
                console.log("courses fetched",response.data);
                
            } catch (error) {
                toast.error(`an error occurred while fetching courses ${error}`)
            }
        }

        fetchCourses()
    },[])
  return (
    <div>
            <div className='md:w-[80%] w-full m-auto mt-10'>
        <div className='w-full flex justify-between'>
        <h2 className="text-2xl font-semibold mb-4 text-gray-800">Select a course</h2>

        
      
        </div>
        <div className="flex w-full flex-row flex-wrap gap-4">
      {courses?.length>0
      ?courses.map((course) => (
        <div
          onClick={() =>
            navigate(
              `/studentDashboard/material/${course.courseId}`
            )
          }
          key={course.courseId}
          className="w-72 bg-white rounded-lg shadow-lg hover:scale-105 hover:shadow-2xl transition-all duration-300 transform cursor-pointer"
        >
          <div className="p-4 space-y-2">
            <h3 className="text-xl font-semibold text-gray-800">
              {course.courseName}
            </h3>
            <p className="text-sm text-gray-600">Code: {course.courseCode}</p>
            <p className="text-sm text-gray-600 capitalize">
              Grade: {course?.grade?.replaceAll("_", " ").toLowerCase()}
            </p>
            <p className="text-sm text-gray-600">
              Semester: {course.semester.semesterName}
            </p>
            <p className="text-sm text-gray-600">
              Year: {course.semester.year}
            </p>
            <div className="flex justify-between items-center mt-2">
              <button className="text-blue-500 hover:underline">
                View Details
              </button>
            </div>
          </div>
     
        </div>
        
      ))
    :
    <div>
          <h2 className="text-xl  mb-4 text-gray-400">
            No Courses Found
          </h2>
     

        </div>
    }


    </div>
        
    </div>
    </div>
  )
}

export default StudentMaterial