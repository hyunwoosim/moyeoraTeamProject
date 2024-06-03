package moyeora.myapp.controller;


import lombok.RequiredArgsConstructor;
import moyeora.myapp.annotation.LoginUser;
import moyeora.myapp.dto.schoolclass.ClassDeleteDTO;
import moyeora.myapp.dto.schoolclass.SchoolClassRequestDTO;
import moyeora.myapp.service.SchoolClassService;
import moyeora.myapp.service.SchoolMemberService;
import moyeora.myapp.util.FileUpload;
import moyeora.myapp.vo.JsonResult;
import moyeora.myapp.vo.SchoolClass;
import moyeora.myapp.vo.User;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

@Controller
@RequestMapping("schoolclass")
@RequiredArgsConstructor
public class SchoolClassController {

  private static final Log log = LogFactory.getLog(SchoolClassController.class);
  private final SchoolClassService schoolClassService;
  private final FileUpload fileUpload;
  private final SchoolMemberService schoolMemberService;
  private final String uploadDir =  "schoolclass/";
  @Value("${ncp.storage.bucket}") private String bucket;


  @GetMapping("list")
  @ResponseBody
  public Object viewOfDate(String date, @LoginUser User loginUser) {
    if (date.matches("\\d{4}-\\d{2}-\\d{2}")) {
      return schoolClassService.findByDate(date, loginUser.getNo());
    } else {
      LocalDateTime currentTime = LocalDateTime.now();
      DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd"); // 날짜 형식 지정
      date = currentTime.format(formatter);
      return schoolClassService.findByDate(date, loginUser.getNo());
    }
  }

  @GetMapping("realCalendar")
  public void form(Model model,@LoginUser User loginUser) throws Exception {
    model.addAttribute("loginUser",loginUser.getNo());

  }

  @GetMapping("calendar")
  @ResponseBody
  public List<SchoolClass> Calendar(int schoolNo) throws Exception {
    return schoolClassService.schoolCalendarList(schoolNo);
  }




  @PostMapping("add")
  @ResponseBody
  public Object add(SchoolClass clazz, MultipartFile file,
                  LocalDateTime startAt2,
                  LocalDateTime endedAt2,
                  ZoneId zoneId,
                  @LoginUser User loginUser,
                    int schoolNo
  ) throws Exception{

    clazz.setUserNo(loginUser.getNo());
    clazz.setSchoolNo(schoolNo);
    if(file.getSize() > 0){
      String filename = fileUpload.upload(this.bucket, this.uploadDir, file);
      clazz.setPhoto(filename);
    }


    Date startAtDate = Date.from(startAt2.atZone(zoneId).toInstant());
    Date endedAtDate = Date.from(endedAt2.atZone(zoneId).toInstant());

    clazz.setStartAt(startAtDate);
    clazz.setEndedAt(endedAtDate);
    schoolClassService.add(clazz);


    JsonResult jsonResult = new JsonResult();
    jsonResult.setStatus("success");

    return jsonResult;

  }

  @GetMapping("findByNo")
  @ResponseBody
  public Object findByNo(int classNo, @LoginUser User loginUser, Model model) throws Exception {
    HashMap<String, Object> result = new HashMap<>();
    result.put("schoolClass", schoolClassService.get(classNo));
    result.put("isMember", schoolClassService.isMember(classNo, loginUser.getNo()));

    return result;
  }

  @GetMapping("findByClassMember")
  @ResponseBody
  public Object findByClassMember(int classNo) throws Exception {
    HashMap<String, Object> result = new HashMap<>();
    result.put("schoolMembers",schoolMemberService.list(classNo));
    return result;
  }

  @PostMapping("insert")
  @ResponseBody
  public Object attend(@RequestBody SchoolClassRequestDTO schoolClassRequestDTO, @LoginUser User loginUser) throws Exception {
    schoolClassRequestDTO.setUserNo(loginUser.getNo());
    schoolClassService.insert(schoolClassRequestDTO);
    return "";
  }


  @PostMapping("memberDelete")
  @ResponseBody
  public Object MemberDelete(@RequestBody SchoolClassRequestDTO schoolClassRequestDTO, @LoginUser User loginUser) throws Exception {
    schoolClassRequestDTO.setUserNo(loginUser.getNo());
    schoolClassService.memberDelete(schoolClassRequestDTO);
    return "";
  }

  @PostMapping("classDelete")
  @ResponseBody
  public void classDelete(@RequestBody SchoolClass clazz, @LoginUser User loginUser) throws Exception {


  if (clazz.getUserNo() == loginUser.getNo()) {


    ClassDeleteDTO classDeleteDTO = new ClassDeleteDTO();
    classDeleteDTO.setClassNo(clazz.getNo());

    classDeleteDTO.setUserNo(loginUser.getNo());

    SchoolClassRequestDTO schoolClassRequestDTO = new SchoolClassRequestDTO();
    schoolClassRequestDTO.setUserNo(loginUser.getNo());
    schoolClassRequestDTO.setSchoolNo(clazz.getSchoolNo());
    schoolClassRequestDTO.setClassNo(classDeleteDTO.getClassNo());

    schoolClassService.memberDelete(schoolClassRequestDTO);
    schoolClassService.classDelete(classDeleteDTO);
  } else {
    throw new Exception("권한이 없습니다.");
  }

  }


    @PostMapping("classUpdate")
    @ResponseBody
    public void update(SchoolClass clazz,
                       MultipartFile file,
                       @LoginUser User loginUser,
                       LocalDateTime startAt3,
                       LocalDateTime endedAt3,
                       ZoneId zoneId) throws Exception {



      SchoolClass old = schoolClassService.get(clazz.getNo());
      if (old == null) {
        throw new Exception("회원 번호가 유효하지 않습니다.");
      }
      clazz.setNo(old.getNo());

      clazz.setCreatedAt(old.getCreatedAt());

      if (file != null) {
        String filename = fileUpload.upload(this.bucket, this.uploadDir, file);
        clazz.setPhoto(filename);
        fileUpload.delete(this.bucket, this.uploadDir, old.getPhoto());
      } else {
        clazz.setPhoto(old.getPhoto());
      }

      Date startAtDate = Date.from(startAt3.atZone(zoneId).toInstant());
      Date endedAtDate = Date.from(endedAt3.atZone(zoneId).toInstant());

      clazz.setStartAt(startAtDate);
      clazz.setEndedAt(endedAtDate);


      schoolClassService.classUpdate(clazz);

    }

}
