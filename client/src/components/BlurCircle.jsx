const BlurCircle = ({ top = "auto", right = "auto", left = "auto", bottom = "auto" }) => {
  return (
    <div
      className="absolute -z-50 h-[400px] w-[400px] aspect-square rounded-full 
      blur-[100px] bg-gradient-to-r from-[#007bff] to-[#00cfff] 
      opacity-50 animate-blob"
      style={{ top, right, left, bottom }}
    />
  )
}

export default BlurCircle
