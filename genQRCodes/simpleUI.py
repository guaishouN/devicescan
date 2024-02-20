import tkinter as tk
from getQRCodeImg import *

window = tk.Tk()
window.title('设备管理标签打印')
window.geometry('485x200')
window.resizable(width=False, height=True)
paned_window = tk.PanedWindow(window, orient=tk.VERTICAL)
paned_window.pack(fill=tk.BOTH, expand=True)

# 上部分
frame_top = tk.Frame(paned_window)
paned_window.add(frame_top)

var = tk.StringVar()
is_Hidden = False


def hidden_me():
    pass


def edit_config_file():
    os.system(r'start ./feishu-config.ini')
    pass


tk.Button(frame_top, text='重置配置', font=("Microsoft YaHei", 10), width=12, height=1, command=reset_config_file) \
    .grid(row=0, column=0, padx=2, pady=2)
tk.Button(frame_top, text='编辑配置', font=("Microsoft YaHei", 10), width=12, height=1, command=edit_config_file) \
    .grid(row=0, column=1, padx=2, pady=2)
tk.Button(frame_top, text='生成打印标签Word文档', bg='green', fg='white', font=("Microsoft YaHei", 10, "bold"),
          width=30, height=1,
          command=do_main) \
    .grid(row=0, column=2, columnspan=10, padx=10, pady=10)

# 下部分
frame_bottom = tk.Frame(paned_window)
paned_window.add(frame_bottom)

# 创建Text控件用于显示日志
text = tk.Text(frame_bottom, wrap='word', height=10, bg='#D7F0FB', fg='gray', font=('Arial', 10))
text.pack(fill=tk.BOTH, expand=True, padx=5)
logger = logging.getLogger()
handler = logging.StreamHandler()
formatter = logging.Formatter('%(asctime)s - %(levelname)s - %(message)s')
handler.setFormatter(formatter)
# 将StreamHandler对象添加到Logger对象中
logger.addHandler(handler)


# 将日志输出到Text控件中
class TextHandler(logging.Handler):
    def __init__(self, text):
        logging.Handler.__init__(self)
        self.text = text

    def emit(self, record):
        msg = self.format(record)
        self.text.insert(tk.END, msg + '\n')
        self.text.see(tk.END)


# 创建一个TextHandler对象
text_handler = TextHandler(text)

# 设置TextHandler对象的Formatter对象
text_handler.setFormatter(formatter)

# 将TextHandler对象添加到Logger对象中
logger.addHandler(text_handler)

# 输出日志
logger.info('初始化化完成！')

window.mainloop()
