import tkinter as tk

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


tk.Button(frame_top, text='重置配置', font=("Microsoft YaHei", 10), width=12, height=1, command=hidden_me) \
    .grid(row=0, column=0, padx=2, pady=2)
tk.Button(frame_top, text='编辑配置', font=("Microsoft YaHei", 10), width=12, height=1, command=hidden_me) \
    .grid(row=0, column=1, padx=2, pady=2)
tk.Button(frame_top, text='生成打印标签Word文档', bg='green', fg='white', font=("Microsoft YaHei", 10, "bold"), width=30, height=1,
          command=hidden_me) \
    .grid(row=0, column=2, columnspan=10, padx=10, pady=10)


# 下部分
frame_bottom = tk.Frame(paned_window)
paned_window.add(frame_bottom)

label = tk.Label(frame_bottom, text='Label', bg='yellow', font=('Arial', 10))
label.pack(fill=tk.BOTH, expand=True, padx=5)

window.mainloop()
