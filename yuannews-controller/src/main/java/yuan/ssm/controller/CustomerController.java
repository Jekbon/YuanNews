package yuan.ssm.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;
import yuan.ssm.common.util.FileTool;
import yuan.ssm.common.util.LoggerUtil;
import yuan.ssm.other.CommentJo;
import yuan.ssm.other.PageVo;
import yuan.ssm.pojo.CSCustom;
import yuan.ssm.pojo.NewsCustom;
import yuan.ssm.service.customer.NewsService;
import yuan.ssm.service.customer.UserService;
import yuan.ssm.vo.TasteVo;
import yuan.ssm.vo.UserVo;

import javax.servlet.http.HttpSession;
import java.util.List;
import java.util.Set;

/**
 * Created by yuan on 16-4-4.
 * 控制器－用户端-用户中心
 * 功能：用户中心页面及其功能
 */


@Controller
@RequestMapping(value = "/html")
public class CustomerController {

    //主页
    private final String INDEX_PAGE="html/index";

    //登陆页面
    private final String LOGIN_PAGE="html/login/login";

    //用户详情页面
    private final String USER_DETAIL="html/login/detail";

    //用户注册页面
    private final String USER_REGISTER="html/login/register";

    //新闻详情页面
    private final String NEWS_DETAIL_PAGE="html/detail";

    //标签页面
    private final String USER_LABEL_PAGE="html/label";

    //推荐页面
    private final String NEWS_TUIJIAN_PAGE="html/tuijian";

    //每页数量
    private final int PAGE_NUM=10;
    //标签每页显示数量
    private final int PAGE_LABEL_NUM=24;
    //ID
    private final int idType=2;
    //阅读
    private final int rnumType=3;
    //点赞
    private final int zanType=4;
    //评论
    private final int commentType=5;



    @Autowired
    private NewsService newsService;

    @Autowired
    private UserService userService;

    private int idsSize=0;


    /**
     * 主页面包括两个内容： 1.新闻展示列表；2.新闻推荐模块；
     *
     * 1.新闻有关的列表页面
     * 包括：
     * 显示页面；
     * 当前页面；
     * 新闻总数；
     * 当前类型：普通，分类，来源；
     * 列表数据；
     * 页面标题；
     * 分类和来源信息；
     * 当前加载的类型：点赞，阅读，评论，普通
     *

     * @param pageVo
     * @return
     * @throws Exception
     */
    @RequestMapping("/pageIndex")
    public ModelAndView pageIndex(@ModelAttribute PageVo pageVo) throws Exception {
        if(pageVo.getP()<0){
            pageVo.setP(1);
        }
        pageVo.setStart((pageVo.getP()-1)*PAGE_NUM);//开始页面
        pageVo.setNum(PAGE_NUM);//每页总数
        LoggerUtil.printJSON(pageVo);
        List<NewsCustom> normalNews = getNormalNews(pageVo);
        Integer allCount = newsService.getNewsCount().getAllCount();
        return getModelAndView(INDEX_PAGE,pageVo,normalNews,allCount);
    }

    /**
     * 正常的读取操作
     * @param customByComment 集合
     * @return
     * @throws Exception
     */
    private ModelAndView getModelAndView(String page,@ModelAttribute PageVo pageVo,List<NewsCustom> customByComment,Integer allCount) throws Exception {
        ModelAndView andView = new ModelAndView();
        //当前页面
        andView.addObject("currectIndex",pageVo.getP()); //当前页面
        //页面
        andView.setViewName(page);//页面对象
        andView.addObject("customs",customByComment);
        //总数
        andView.addObject("count",allCount);
        //分类/来源数据
        CSCustom sourceIfo = newsService.findCateSourceIfo();
        andView.addObject("sourceIfo",sourceIfo);
        andView.addObject("title",pageVo.getTitle()==null?"新闻推荐":pageVo.getTitle());
        andView.addObject("ntype",pageVo.getnType());
        andView.addObject("type",pageVo.getType());
        return andView;
    }

    /**
     *  2.推荐模块-用户登陆后的操作
     *  (1)读取本地用户的推荐新闻id
     *  (2)文件没有,返回null,执行 normal, 用户没有登陆执行normal
     *  (3)文件存在,根据p;来判断；if start>size normal
     *  (4)推荐下，不进行点赞/热度/评论
     * @param pageVo
     * @return
     * @throws Exception
     */
    @RequestMapping("/tuiJianIndexPage")
    public ModelAndView tuiJianIndexPage(HttpSession session, @ModelAttribute PageVo pageVo) throws Exception {
        UserVo userVo= (UserVo) session.getAttribute("user");
        if(userVo==null){
            return new ModelAndView("redirect:/html/pageIndex.action?p=1&type=2&nType=6");
        }
        if(userVo.getId()==0){
            return new ModelAndView("redirect:/html/pageIndex.action?p=1&type=2&nType=6");
        }

        if(pageVo.getP()<0){
            pageVo.setP(1);
        }
        pageVo.setStart((pageVo.getP()-1)*PAGE_NUM);//开始页面
        pageVo.setNum(PAGE_NUM);//每页总数
        LoggerUtil.printJSON(pageVo);

        //读取
        List<Integer> newsIds = FileTool.readData(userVo.getId());
        //没有nid
        if(newsIds==null){
            return new ModelAndView("redirect:/html/pageIndex.action?p=1&type=2&nType=6");
        }
        //nid=0; 或者 nid 不足一页：不显示数据
        if(newsIds.size()==0 || newsIds.size()<pageVo.getNum()){
            return new ModelAndView("redirect:/html/pageIndex.action?p=1&type=2&nType=6");
        }

        try {
            int endIndex=pageVo.getStart()+pageVo.getNum();
            if(endIndex>newsIds.size()){
                return new ModelAndView("redirect:/html/pageIndex.action?p=1&type=2&nType=6");
            }
            int count=newsIds.size()/pageVo.getNum()*pageVo.getNum();
            List<Integer> nids = newsIds.subList(pageVo.getStart(),endIndex);
            List<NewsCustom> nidsNews = newsService.getNidsNews(nids, pageVo.getType());
            return getModelAndView(NEWS_TUIJIAN_PAGE,pageVo,nidsNews,count);
        }catch (Exception e){
            return new ModelAndView("redirect:/html/pageIndex.action?p=1&type=2&nType=6");
        }
    }



    /**
     * 正常的获取类型
     * @param pageVo
     * @return
     * @throws Exception
     */
    private List<NewsCustom>  getNormalNews(@ModelAttribute PageVo pageVo) throws Exception {
        //list数据
        switch (pageVo.getType()) {
            case idType:
                return newsService.getIdNews(pageVo);
            case rnumType:
                return newsService.getRnumNews(pageVo);
            case zanType:
                return newsService.getZanNews(pageVo);
            case commentType:
                return newsService.getCommentNews(pageVo);
            default:
                return null;
        }
    }


    /**
     * 用户登陆页面
     * @return
     */
    @RequestMapping("login/userLoginPage")
    public String userLoginPage(){
        return LOGIN_PAGE;
    }

    /**
     * 用户登陆
     * @param session
     * @param unum
     * @param pass
     * @return 登陆成功 1 ，失败-1
     * @throws Exception
     */
    @RequestMapping("login/userLogin")
    public @ResponseBody Integer userLogin(HttpSession session, @RequestParam String unum,@RequestParam String pass) throws Exception {
        UserVo vo = userService.userLogin(unum, pass);
        if(vo!=null) {
            session.setAttribute("user", vo);
            return 1;
        }else{
            return -1;
        }
    }

    /**
     * 用户登出
     * @param session
     * @return
     */
    @RequestMapping("login/userLoignout")
    public @ResponseBody Integer userLoignout(HttpSession session){
        session.invalidate();
        return 0;
    }

    /**
     * 用户详情页面
     * @return
     */
    @RequestMapping("login/userDetail")
    public ModelAndView userDetail(HttpSession session) throws Exception {
        //查询标签数据
        UserVo vo= (UserVo) session.getAttribute("user");
        int id = vo.getId();
        List<TasteVo> tasteVos = userService.userSelectTasteById(id);
        ModelAndView andView=new ModelAndView();
        andView.addObject("tasteVos",tasteVos);
        andView.setViewName(USER_DETAIL);
        return andView;
    }

    /**
     * 用户注册页面
     * @return
     */
    @RequestMapping("login/userRegesterPage")
    public String userRegesterPage(){
        return USER_REGISTER;
    }

    /**
     * 用户注册
     * @param userVo
     * @return
     * @throws Exception
     */
    @RequestMapping("login/userRegister")
    public @ResponseBody Integer userRegister(@ModelAttribute UserVo userVo) throws Exception {
        LoggerUtil.print(userVo);
      return userService.userRegister(userVo.getUnum(),userVo.getPass(),userVo.getNick());
    }

    /**
     * 删除兴趣标签
     * @param id
     * @return
     * @throws Exception
     */
    @RequestMapping("login/userEditLabel")
    public @ResponseBody Integer userEditLabel(@RequestParam Integer id) throws Exception {
        return userService.userDeleteTasteById(id);
    }

    /**
     * 添加兴趣标签
     * @param session
     * @param label
     * @return
     * @throws Exception
     */
    @RequestMapping("login/userAddLabel")
    public @ResponseBody Integer userAddLabel(HttpSession session,@RequestParam String label) throws Exception{
        UserVo vo= (UserVo) session.getAttribute("user");
        int id = vo.getId();
        if(userService.userCheckTaste(label,id)){
            return -1;
        }
       return userService.userAddTasteLabel(label,id);
    }

    /**
     * 新闻详情页面
     *  需要数据：
     *   新闻详情
     *   点赞状态（状态当前用户点赞状态（只能异步获取））
     *   评论分页
     *   评论总数
     *   点赞的用户信息
     *   阅读量+1
     *   分类和来源信息
     *   评论业务
     * @return
     */
    @RequestMapping("/newsDetailPage")
    public ModelAndView newsDetailPage(@RequestParam Integer p,@RequestParam Integer nid) throws Exception {
        if(p<0){
            p=1;
        }
        int currentPage=p;
        p=PAGE_NUM*(p-1);

        ModelAndView andView = new ModelAndView();
        andView.setViewName(NEWS_DETAIL_PAGE);
        //分类/来源数据
        CSCustom sourceIfo = newsService.findCateSourceIfo();
        andView.addObject("sourceIfo",sourceIfo);
        //新闻详情
        NewsCustom newsCustom = userService.selectNewsDetailById(nid);
        andView.addObject("newsCustom",newsCustom);
        //评论总数
        andView.addObject("count",newsCustom.getCnum());
        //当前页
        andView.addObject("currentPage",currentPage);
        //评论
        List<CommentJo> commentJos = userService.selectComments(p, PAGE_NUM, nid);
        andView.addObject("commentJos",commentJos);
        //点赞人信息
        List<String> heads = userService.selectLikedByNid(nid);
        andView.addObject("heads",heads);

        //阅读数+1
        newsService.updateNewsRnum(nid);

        return andView;
    }

    /**
     * 获取点赞状态
     * @param uid 用户id
     * @param nid 新闻id
     * @return
     * @throws Exception
     */
    @RequestMapping("/getLikedStatus")
    public @ResponseBody Integer getLikedStatus(@RequestParam Integer uid,@RequestParam Integer nid) throws Exception {
        return userService.selectZanStatus(uid,nid);
    }

    /**
     * 点赞
     * @param uid 用户id
     * @param nid 新闻id
     * @param status 当前状态
     * @return
     * @throws Exception
     */
    @RequestMapping("/userZan")
    public @ResponseBody Integer userZan(@RequestParam Integer uid,@RequestParam Integer nid,@RequestParam Integer status) throws Exception {
        return userService.userZanNews(uid, nid, status);
    }

    /**
     * 用户评论实现
     * @param uid
     * @param nid
     * @param content
     * @return
     * @throws Exception
     */
    @RequestMapping("/userComment")
    public @ResponseBody Integer userComment(@RequestParam Integer uid,@RequestParam Integer nid,@RequestParam String content) throws Exception {
       return userService.userCommentNews(uid,nid,content);
    }


    /**
     * 标签页面内容
     * 策略：如果用户没有登陆，查询全部，如果用户登陆了，查询不是用户的标签
     * @param p 当前页面
     * @return
     * @throws Exception
     */
    @RequestMapping("/userLabelPage")
    public ModelAndView userLabelPage(HttpSession session,@RequestParam Integer p) throws Exception {

        UserVo vo= (UserVo) session.getAttribute("user");
        if(vo==null){
            vo=new UserVo();
            vo.setId(0);
        }

        if(p==null){
            p=1;
        }

        if(p<0){
            p=1;
        }
        int currentPage=p;
        p=(currentPage-1)*PAGE_LABEL_NUM;

        ModelAndView andView = new ModelAndView();
        andView.setViewName(USER_LABEL_PAGE);
        //分类/来源数据
        CSCustom sourceIfo = newsService.findCateSourceIfo();
        andView.addObject("sourceIfo",sourceIfo);
        //当前页面
        andView.addObject("currentPage",currentPage);
        //总数
        Integer count = userService.selectTasteCount();
        andView.addObject("count",count);
        //当前的数据的信息
        Set<String> tastes = userService.selectTaste(p, PAGE_LABEL_NUM, vo.getId());
        andView.addObject("tasteVos",tastes);
        //当前用户已经关注的总数
        if(vo.getId()==0){
            andView.addObject("labelCount",-1);
        }else{
            Integer tasteCount = userService.userSelectTasteCount(vo.getId());
            andView.addObject("labelCount",tasteCount);
        }
        return andView;
    }

}
